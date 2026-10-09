package com.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The "link" entity: one row = one book borrowed by one member.
 * It owns the two FOREIGN KEYS (member_id and book_id).
 */
@Entity
@Table(name = "book_issue")
public class BookIssue {

    public static final String STATUS_ISSUED = "ISSUED";
    public static final String STATUS_RETURNED = "RETURNED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long issueId;

    /** Many issues -> one member. LAZY: the Member row is fetched only when needed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** Many issues -> one book. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "fine_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    public BookIssue() {
    }

    public BookIssue(Member member, Book book, LocalDate issueDate, LocalDate dueDate) {
        this.member = member;
        this.book = book;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = STATUS_ISSUED;
        this.fineAmount = BigDecimal.ZERO;
    }

    public Long getIssueId() {
        return issueId;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(BigDecimal fineAmount) {
        this.fineAmount = fineAmount;
    }

    /** Uses getBookId()/getMemberId() only: reading an id does not initialise a lazy proxy. */
    @Override
    public String toString() {
        return "BookIssue{id=" + issueId
                + ", bookId=" + (book == null ? null : book.getBookId())
                + ", memberId=" + (member == null ? null : member.getMemberId())
                + ", issued=" + issueDate + ", due=" + dueDate + ", returned=" + returnDate
                + ", status=" + status + ", fine=" + fineAmount + "}";
    }
}
