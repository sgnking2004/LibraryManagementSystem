package com.library.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * ORM mapping: one Member object  <->  one row in the MEMBER table.
 */
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    /**
     * One member -> many issue records.
     * mappedBy = "member" means the FOREIGN KEY lives in BOOK_ISSUE (field BookIssue.member),
     * so this side is only a read-friendly "view" of the relationship.
     * LAZY = the issue list is loaded from the database only when you actually use it.
     * Cascade PERSIST/MERGE = saving a member also saves new issues added to this list.
     * (We do NOT cascade REMOVE: deleting a member must never silently delete history.)
     */
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<BookIssue> issues = new ArrayList<>();

    public Member() {
    }

    public Member(String name, String email, String phone, String address) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    /** Keeps both sides of the relationship in sync. */
    public void addIssue(BookIssue issue) {
        issues.add(issue);
        issue.setMember(this);
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<BookIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<BookIssue> issues) {
        this.issues = issues;
    }

    /** The lazy 'issues' list is deliberately NOT printed (it would trigger a database query). */
    @Override
    public String toString() {
        return "Member{id=" + memberId + ", name='" + name + "', email='" + email
                + "', phone='" + phone + "', address='" + address + "'}";
    }
}
