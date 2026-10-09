package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.BookIssueDAO;
import com.library.dao.MemberDAO;
import com.library.entity.Book;
import com.library.entity.BookIssue;
import com.library.entity.Member;
import com.library.exception.LibraryException;
import com.library.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Business layer: the library RULES live here.
 * Main talks to this class; this class talks to the DAOs.
 */
public class LibraryService {

    public static final int LOAN_DAYS = 14;
    public static final BigDecimal FINE_PER_DAY = new BigDecimal("5");   // Rs. 5 per late day

    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final BookIssueDAO issueDAO = new BookIssueDAO();

    // ------------------------------------------------------------------
    // BOOKS
    // ------------------------------------------------------------------

    public Book addBook(String title, String author, String isbn, String category, int quantity) {
        requireText(title, "Title");
        requireText(author, "Author");
        requireText(isbn, "ISBN");
        if (quantity < 1) {
            throw new LibraryException("Quantity must be at least 1.");
        }
        return bookDAO.addBook(new Book(title.trim(), author.trim(), isbn.trim(),
                category == null ? null : category.trim(), quantity));
    }

    public Book getBook(Long id) {
        return bookDAO.getBookById(id);
    }

    public List<Book> getAllBooks() {
        return bookDAO.getAllBooks();
    }

    public List<Book> searchBooksByTitle(String text) {
        return bookDAO.searchBooksByTitle(text.trim());
    }

    public List<Book> searchBooksByAuthor(String text) {
        return bookDAO.searchBooksByAuthor(text.trim());
    }

    public List<Book> getAvailableBooks() {
        return bookDAO.getAvailableBooks();
    }

    public long countBooks() {
        return bookDAO.countBooks();
    }

    /** A null or blank argument means "keep the old value". */
    public Book updateBook(Long id, String title, String author, String isbn, String category,
                           Integer newQuantity) {
        Book book = bookDAO.getBookById(id);
        if (book == null) {
            throw new LibraryException("Book not found with ID " + id + ".");
        }
        if (!isBlank(title)) {
            book.setTitle(title.trim());
        }
        if (!isBlank(author)) {
            book.setAuthor(author.trim());
        }
        if (!isBlank(isbn)) {
            book.setIsbn(isbn.trim());
        }
        if (!isBlank(category)) {
            book.setCategory(category.trim());
        }
        if (newQuantity != null) {
            // Copies currently borrowed = quantity - available. They stay borrowed.
            int borrowed = book.getQuantity() - book.getAvailableQuantity();
            if (newQuantity < borrowed) {
                throw new LibraryException("Quantity cannot be less than the " + borrowed
                        + " copies that are currently issued.");
            }
            book.setQuantity(newQuantity);
            book.setAvailableQuantity(newQuantity - borrowed);
        }
        return bookDAO.updateBook(book);
    }

    public void deleteBook(Long id) {
        if (!bookDAO.deleteBook(id)) {
            throw new LibraryException("Book not found with ID " + id + ".");
        }
    }

    // ------------------------------------------------------------------
    // MEMBERS
    // ------------------------------------------------------------------

    public Member addMember(String name, String email, String phone, String address) {
        requireText(name, "Name");
        requireText(email, "Email");
        validateEmail(email);
        return memberDAO.addMember(new Member(name.trim(), email.trim(),
                isBlank(phone) ? null : phone.trim(), isBlank(address) ? null : address.trim()));
    }

    public Member getMember(Long id) {
        return memberDAO.getMemberById(id);
    }

    public List<Member> getAllMembers() {
        return memberDAO.getAllMembers();
    }

    public List<Member> searchMemberByName(String text) {
        return memberDAO.searchMemberByName(text.trim());
    }

    /** A null or blank argument means "keep the old value". */
    public Member updateMember(Long id, String name, String email, String phone, String address) {
        Member member = memberDAO.getMemberById(id);
        if (member == null) {
            throw new LibraryException("Member not found with ID " + id + ".");
        }
        if (!isBlank(name)) {
            member.setName(name.trim());
        }
        if (!isBlank(email)) {
            validateEmail(email);
            member.setEmail(email.trim());
        }
        if (!isBlank(phone)) {
            member.setPhone(phone.trim());
        }
        if (!isBlank(address)) {
            member.setAddress(address.trim());
        }
        return memberDAO.updateMember(member);
    }

    public void deleteMember(Long id) {
        if (!memberDAO.deleteMember(id)) {
            throw new LibraryException("Member not found with ID " + id + ".");
        }
    }

    // ------------------------------------------------------------------
    // ISSUE / RETURN  (one transaction each)
    // ------------------------------------------------------------------

    public BookIssue issueBook(Long memberId, Long bookId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            // 1. find the member
            Member member = session.find(Member.class, memberId);
            if (member == null) {
                throw new LibraryException("Member not found with ID " + memberId + ".");
            }

            // 2-3. find the book
            Book book = session.find(Book.class, bookId);
            if (book == null) {
                throw new LibraryException("Book not found with ID " + bookId + ".");
            }

            // 4. take one copy atomically. The UPDATE only succeeds while a copy is still available,
            //    so two users can never take the last copy (works on both MariaDB and MySQL).
            int taken = session.createMutationQuery(
                            "update Book b set b.availableQuantity = b.availableQuantity - 1 "
                                    + "where b.bookId = :id and b.availableQuantity > 0")
                    .setParameter("id", bookId)
                    .executeUpdate();
            if (taken == 0) {
                throw new LibraryException("Sorry, \"" + book.getTitle()
                        + "\" is currently unavailable. All copies are issued.");
            }

            session.refresh(book);   // reload the new available_quantity into the Book object

            // 5-8. create the issue record
            LocalDate today = LocalDate.now();
            BookIssue issue = new BookIssue(member, book, today, today.plusDays(LOAN_DAYS));
            issueDAO.issueBook(session, issue);

            // 9. available_quantity was already decreased by the UPDATE in step 4

            // 10. commit both changes together
            tx.commit();
            return issue;
        } catch (LibraryException e) {
            rollback(tx);
            throw e;
        } catch (RuntimeException e) {
            rollback(tx);
            throw new LibraryException("Transaction failed. Nothing was saved. Reason: " + e.getMessage(), e);
        }
    }

    public BookIssue returnBook(Long issueId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            // 1. find the issue record
            BookIssue issue = issueDAO.getIssueById(session, issueId);
            if (issue == null) {
                throw new LibraryException("Invalid issue ID " + issueId + ": no such issue record.");
            }

            // 2. must still be ISSUED (prevents returning twice)
            if (!BookIssue.STATUS_ISSUED.equals(issue.getStatus())) {
                throw new LibraryException("Issue " + issueId + " was already returned on "
                        + issue.getReturnDate() + ".");
            }

            // 3-4. return date, status and fine
            LocalDate today = LocalDate.now();
            issue.setReturnDate(today);
            issue.setStatus(BookIssue.STATUS_RETURNED);
            issue.setFineAmount(calculateFine(issue.getDueDate(), today));
            issueDAO.returnBook(session, issue);

            // 5. put the copy back on the shelf
            session.createMutationQuery(
                            "update Book b set b.availableQuantity = b.availableQuantity + 1 "
                                    + "where b.bookId = :id")
                    .setParameter("id", issue.getBook().getBookId())
                    .executeUpdate();
            session.refresh(issue.getBook());   // reload the new available_quantity

            // 6. commit
            tx.commit();
            return issue;
        } catch (LibraryException e) {
            rollback(tx);
            throw e;
        } catch (RuntimeException e) {
            rollback(tx);
            throw new LibraryException("Transaction failed. Nothing was saved. Reason: " + e.getMessage(), e);
        }
    }

    /** Rs. 5 for every day after the due date; Rs. 0 if returned on or before it. */
    public static BigDecimal calculateFine(LocalDate dueDate, LocalDate returnDate) {
        long lateDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        if (lateDays <= 0) {
            return BigDecimal.ZERO;
        }
        return FINE_PER_DAY.multiply(BigDecimal.valueOf(lateDays));
    }

    // ------------------------------------------------------------------
    // ISSUE QUERIES
    // ------------------------------------------------------------------

    public BookIssue getIssue(Long id) {
        return issueDAO.getIssueById(id);
    }

    public List<BookIssue> getAllIssues() {
        return issueDAO.getAllIssues();
    }

    public List<BookIssue> getActiveIssues() {
        return issueDAO.getActiveIssues();
    }

    public List<BookIssue> getOverdueIssues() {
        return issueDAO.getOverdueIssues();
    }

    public List<BookIssue> getReturnedIssues() {
        return issueDAO.getReturnedIssues();
    }

    public List<BookIssue> getIssuesByMember(Long memberId) {
        requireMemberExists(memberId);
        return issueDAO.getIssuesByMember(memberId);
    }

    public List<BookIssue> getIssueHistory(Long memberId) {
        requireMemberExists(memberId);
        return issueDAO.getIssueHistory(memberId);
    }

    public long countActiveIssues() {
        return issueDAO.countActiveIssues();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void requireMemberExists(Long memberId) {
        if (memberDAO.getMemberById(memberId) == null) {
            throw new LibraryException("Member not found with ID " + memberId + ".");
        }
    }

    private void requireText(String value, String field) {
        if (isBlank(value)) {
            throw new LibraryException(field + " cannot be empty.");
        }
    }

    private void validateEmail(String email) {
        if (!email.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new LibraryException("Invalid email format: " + email);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void rollback(Transaction tx) {
        if (tx != null && tx.isActive()) {
            tx.rollback();
        }
    }
}
