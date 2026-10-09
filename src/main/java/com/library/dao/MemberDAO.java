package com.library.dao;

import com.library.entity.Member;
import com.library.exception.LibraryException;
import com.library.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/** Data Access Object for Member (same Session/Transaction pattern as BookDAO). */
public class MemberDAO {

    public Member addMember(Member member) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(member);
            tx.commit();
            return member;
        } catch (RuntimeException e) {
            rollback(tx);
            throw new LibraryException("Could not add the member (is the email already used?).", e);
        }
    }

    public Member getMemberById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Member.class, id);
        } catch (RuntimeException e) {
            throw new LibraryException("Could not read the member from the database.", e);
        }
    }

    public List<Member> getAllMembers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Member m order by m.name", Member.class).list();
        } catch (RuntimeException e) {
            throw new LibraryException("Could not read the list of members.", e);
        }
    }

    public Member updateMember(Member member) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Member updated = session.merge(member);
            tx.commit();
            return updated;
        } catch (RuntimeException e) {
            rollback(tx);
            throw new LibraryException("Could not update the member (is the email already used?).", e);
        }
    }

    /** A member who has issue records (even returned ones) is kept, to protect borrowing history. */
    public boolean deleteMember(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Member member = session.get(Member.class, id);
            if (member == null) {
                tx.rollback();
                return false;
            }
            Long issueCount = session.createQuery(
                            "select count(bi) from BookIssue bi where bi.member.memberId = :id", Long.class)
                    .setParameter("id", id)
                    .uniqueResult();
            if (issueCount != null && issueCount > 0) {
                tx.rollback();
                throw new LibraryException("This member has " + issueCount
                        + " issue record(s) and cannot be deleted.");
            }
            session.remove(member);
            tx.commit();
            return true;
        } catch (LibraryException e) {
            throw e;
        } catch (RuntimeException e) {
            rollback(tx);
            throw new LibraryException("Could not delete the member.", e);
        }
    }

    public List<Member> searchMemberByName(String name) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Member m where lower(m.name) like :n order by m.name", Member.class)
                    .setParameter("n", "%" + name.toLowerCase() + "%")
                    .list();
        } catch (RuntimeException e) {
            throw new LibraryException("Member search failed.", e);
        }
    }

    private void rollback(Transaction tx) {
        if (tx != null && tx.isActive()) {
            tx.rollback();
        }
    }
}
