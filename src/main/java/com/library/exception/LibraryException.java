package com.library.exception;

/**
 * One simple exception type for every business or database problem.
 * The message is always written so that it can be shown directly to the console user.
 */
public class LibraryException extends RuntimeException {

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
