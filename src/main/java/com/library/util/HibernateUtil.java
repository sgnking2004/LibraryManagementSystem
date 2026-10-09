package com.library.util;

import com.library.exception.LibraryException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Builds ONE SessionFactory for the whole application.
 * A SessionFactory is expensive to create and thread-safe, so it is created once and reused.
 * Each database operation then opens its own cheap, short-lived Session from it.
 */
public final class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            sessionFactory = buildSessionFactory();
        }
        return sessionFactory;
    }

    private static SessionFactory buildSessionFactory() {
        try {
            // Reads src/main/resources/hibernate.cfg.xml
            Configuration cfg = new Configuration().configure();

            // Optional overrides so you never have to type a password into source code.
            String url = System.getenv("DB_URL");
            String user = System.getenv("DB_USER");
            String password = System.getenv("DB_PASSWORD");
            if (url != null && !url.isBlank()) {
                cfg.setProperty("hibernate.connection.url", url);
            }
            if (user != null && !user.isBlank()) {
                cfg.setProperty("hibernate.connection.username", user);
            }
            if (password != null) {
                cfg.setProperty("hibernate.connection.password", password);
            }
            return cfg.buildSessionFactory();
        } catch (Exception e) {
            throw new LibraryException("Could not connect to the database. Is MySQL/MariaDB running in XAMPP, "
                    + "does 'library_db' exist, and are the username/password correct? Reason: "
                    + rootCauseMessage(e), e);
        }
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    /** Call once when the program exits to release all database connections. */
    public static synchronized void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}
