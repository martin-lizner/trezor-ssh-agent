package com.trezoragent.exception;

/**
 *
 * @author Martin Lizner
 * 
 */
public class KeyStoreLoadException extends Exception{
    public KeyStoreLoadException(String message) {
        super(message);
    }
    public KeyStoreLoadException(Throwable ex) {
        super(ex);
    }
    
    
}
