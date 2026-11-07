package com.college.resourcefinder.exception;

/**
 * Thrown when file storage operations fail (upload, save, read, or delete).
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
