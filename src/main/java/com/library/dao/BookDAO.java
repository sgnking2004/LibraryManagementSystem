package com.library.dao;

import com.library.entity.Book;
import com.library.exception.LibraryException;
import com.library.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for Book: all database work for books lives here.
 * Pattern used in every write method:
 *   open Session -> begin Transaction -> do work -> commit
 *   on any error -> rollback -> throw LibraryException
 *   finally the try-with-resources closes the Session.
 */
public class BookDAO {

    /** CREATE */
    public Book addBook(Book book) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(book);               // INSERT; book gets its generated id
            tx.commit();
            return book;
        } catch (RuntimeException e) {
            rollback(tx);
            throw new LibraryException("Could not add the book (is the ISBN already used?).", e);
        }
    }

    /** READ one (returns null if not found) */
    public Book getBookById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Book.class, id);  // SELECT ... WHERE book_id = ?
        } catch (RuntimeException e) {
            throw new LibraryException("Could not read the book from the database.", e);
        }
    }

    /** READ all */
    public List<Book> getAllBooks() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Book b order by b.title", Book.class).list();
        } catch (RuntimeException e) {
            throw new LibraryException("Could not read the list of books.", e);
        }
    }

    /** UPDATE: merge copies the state of a detached Book onto the row with the same id. */
    public Book updateBook(Book book) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Book updated = session.merge(book);  // UPDATE book SET ... WHERE book_id = ?
            tx.commit();
            return updated;
        } catch (RuntimeException e) {
            rollback(tx);
            throw new LibraryException("Could not update the book (is the ISBN already used?).", e);
        }
    }

    /**
     * DELETE. A book that has issue records cannot be deleted, because the
     * foreign key in BOOK_ISSUE would be broken; we check first and give a clear message.
     */
    public boolean deleteBook(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Book book = session.get(Book.class, id);
            if (book == null) {
                tx.rollback();
                return false;
            }
            Long issueCount = session.createQuery(
                            "select count(bi) from BookIssue bi where bi.book.bookId = :id", Long.class)
                    .setParameter("id", id)
                    .uniqueResult();
            if (issueCount != null && issueCount > 0) {
                tx.rollback();
                throw new LibraryException("This book has " + issueCount
                        + " issue record(s) and cannot be deleted.");
            }
            session.remove(book);                // DELETE FROM book WHERE book_id = ?
            tx.commit();
            return true;
        } catch (LibraryException e) {
            throw e;
        } catch (RuntimeException e) {
            rollback(tx);
            throw new LibraryException("Could not delete the book.", e);
        }
    }

    /** HQL #1: books whose title contains the text (case-insensitive). */
    public List<Book> searchBooksByTitle(String title) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Book b where lower(b.title) like :t order by b.title", Book.class)
                    .setParameter("t", "%" + title.toLowerCase() + "%")
                    .list();
        } catch (RuntimeException e) {
            throw new LibraryException("Book search by title failed.", e);
        }
    }

    /** HQL #2: books whose author contains the text (case-insensitive). */
    public List<Book> searchBooksByAuthor(String author) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Book b where lower(b.author) like :a order by b.title", Book.class)
                    .setParameter("a", "%" + author.toLowerCase() + "%")
                    .list();
        } catch (RuntimeException e) {
            throw new LibraryException("Book search by author failed.", e);
        }
    }

    /** HQL #3: books that still have at least one copy on the shelf. */
    public List<Book> getAvailableBooks() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Book b where b.availableQuantity > 0 order by b.title", Book.class)
                    .list();
        } catch (RuntimeException e) {
            throw new LibraryException("Could not read available books.", e);
        }
    }

    /** HQL #8: total number of book titles. */
    public long countBooks() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("select count(b) from Book b", Long.class).uniqueResult();
            return count == null ? 0 : count;
        } catch (RuntimeException e) {
            throw new LibraryException("Could not count books.", e);
        }
    }

    private void rollback(Transaction tx) {
        if (tx != null && tx.isActive()) {
            tx.rollback();
        }
    }
}
