package com.example.inventory.exception;

public class InvalidPriceRangeException extends RuntimeException {
    public InvalidPriceRangeException() {
        super("Minimum price must be less than or equal to maximum price, and both must be non-negative");
    }
}
