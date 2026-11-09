package com.outreach.sysdesign.exception;

/**
 * The uploaded sheet couldn't be read (wrong file type, missing required columns, empty file).
 */
public class SdBadImportException extends RuntimeException {
    public SdBadImportException(String message) {
        super(message);
    }
}
