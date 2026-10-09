package com.library.dao;

import com.library.entity.BookIssue;
import com.library.exception.LibraryException;
import com.library.util.HibernateUtil;
import org.hibernate.Session;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object for BookIssue.
 *
 * issueBook() and returnBook() take a Session that belongs to the CALLER (LibraryService).
 * Issuing/returning changes two tables (book_issue and book) and must be ONE transaction,
 * so the service opens the transaction and these methods just take part in it.
 *
 * All read methods use "join fetch" so the Book and Member of each issue are loaded in the
 * same SQL query. Without it, touching issue.getBook().getTitle() after the Session is closed
 * would throw LazyInitializationException (the relationships are LAZY).
 */
public class BookIssueDAO {

    private static final String FETCH = "select bi from BookIssue bi "
            + "join fetch bi.book join fetch bi.member ";

    /** Saves a new issue inside the caller's transaction. */
    public void issueBook(Session session, BookIssue issue) {
        session.persist(issue);
    }

    /** Saves the returned state of an issue inside the caller's transaction. */
    public void returnBook(Session session, BookIssue issue) {
        session.merge(issue);
    }

    /** Same as getIssueById(Long) but inside an already open Session. */
    public BookIssue getIssueById(Session session, Long id) {
        return session.createQuery(FETCH + "where bi.issueId = :id", BookIssue.class)
                .setParameter("id", id)
                .uniqueResult();
    }

    public BookIssue getIssueById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return getIssueById(session, id);
        } catch (RuntimeException e) {
            throw new LibraryException("Could not read the issue record.", e);
        }
    }

    public List<BookIssue> getAllIssues() {
        return runList(FETCH + "order by bi.issueId desc", null, null);
    }

    /** HQL #4: all issues that have not been returned yet. */
    public List<BookIssue> getActiveIssues() {
        return runList(FETCH + "where bi.status = :s order by bi.dueDate", "s", BookIssue.STATUS_ISSUED);
    }

    /** HQL #5: every issue (active and returned) of one member. */
    public List<BookIssue> getIssuesByMember(Long memberId) {
        return runList(FETCH + "where bi.member.memberId = :m order by bi.issueDate desc", "m", memberId);
    }

    /** Borrowing history of one member = only the books already returned. */
    public List<BookIssue> getIssueHistory(Long memberId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(FETCH
                            + "where bi.member.memberId = :m and bi.status = :s order by bi.returnDate desc",
                            BookIssue.class)
                    .setParameter("m", memberId)
                    .setParameter("s", BookIssue.STATUS_RETURNED)
                    .list();
        } catch (RuntimeException e) {
            throw new LibraryException("Could not read the borrowing history.", e);
        }
    }

    /** HQL #6: still ISSUED and past the due date. */
    public List<BookIssue> getOverdueIssues() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(FETCH
                            + "where bi.status = :s and bi.dueDate < :today order by bi.dueDate",
                            BookIssue.class)
                    .setParameter("s", BookIssue.STATUS_ISSUED)
                    .setParameter("today", LocalDate.now())
                    .list();
        } catch (RuntimeException e) {
            throw new LibraryException("Could not read overdue issues.", e);
        }
    }

    /** HQL #7: issues that have been returned. */
    public List<BookIssue> getReturnedIssues() {
        return runList(FETCH + "where bi.status = :s order by bi.returnDate desc", "s",
                BookIssue.STATUS_RETURNED);
    }

    /** HQL #9: number of books currently out. */
    public long countActiveIssues() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                            "select count(bi) from BookIssue bi where bi.status = :s", Long.class)
                    .setParameter("s", BookIssue.STATUS_ISSUED)
                    .uniqueResult();
            return count == null ? 0 : count;
        } catch (RuntimeException e) {
            throw new LibraryException("Could not count active issues.", e);
        }
    }

    private List<BookIssue> runList(String hql, String paramName, Object paramValue) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            var query = session.createQuery(hql, BookIssue.class);
            if (paramName != null) {
                query.setParameter(paramName, paramValue);
            }
            return query.list();
        } catch (RuntimeException e) {
            throw new LibraryException("Could not read issue records.", e);
        }
    }
}
