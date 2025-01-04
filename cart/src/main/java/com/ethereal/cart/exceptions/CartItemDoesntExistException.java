package com.ethereal.cart.exceptions;

public class CartItemDoesntExistException extends RuntimeException {
    public CartItemDoesntExistException() {
        super();
    }

    public CartItemDoesntExistException(String message) {
        super(message);
    }

    public CartItemDoesntExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public CartItemDoesntExistException(Throwable cause) {
        super(cause);
    }
}
