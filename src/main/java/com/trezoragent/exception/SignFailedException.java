package com.trezoragent.exception;

/**
 *
 * @author martin.lizner
 */
public class SignFailedException extends Exception {

    public SignFailedException(String s) {
        super(s);
    }

    public SignFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
