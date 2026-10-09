package com.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * ORM mapping: one Book object  <->  one row in the BOOK table.
 */
@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // AUTO_INCREMENT primary key
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "author", nullable = false, length = 150)
    private String author;

    @Column(name = "isbn", nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    /** Hibernate needs a no-argument constructor. */
    public Book() {
    }

    public Book(String title, String author, String isbn, String category, int quantity) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.quantity = quantity;
        this.availableQuantity = quantity;   // a new book starts with every copy available
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    @Override
    public String toString() {
        return "Book{id=" + bookId + ", title='" + title + "', author='" + author + "', isbn='" + isbn
                + "', category='" + category + "', quantity=" + quantity
                + ", available=" + availableQuantity + "}";
    }
}
