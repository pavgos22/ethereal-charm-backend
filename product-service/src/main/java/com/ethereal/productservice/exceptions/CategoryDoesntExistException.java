package com.ethereal.productservice.exceptions;

public class CategoryDoesntExistException extends RuntimeException {
    public CategoryDoesntExistException() {
        super();
    }

    public CategoryDoesntExistException(String message) {
        super(message);
    }

    public CategoryDoesntExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public CategoryDoesntExistException(Throwable cause) {
        super(cause);
    }
}
