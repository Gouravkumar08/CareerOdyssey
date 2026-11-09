package com.outreach.dsa.exception;

/**
 * The uploaded sheet couldn't be read (wrong file type, missing required columns, empty file).
 */
public class BadImportException extends RuntimeException {
    public BadImportException(String message) {
        super(message);
    }
}
