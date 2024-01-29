package com.example.demo.exceptions;

public class PlanAlreadyExistsException extends RuntimeException {
    private String message;

    public PlanAlreadyExistsException(String message) {
        super(message);
        this.message = message;
    }

    public PlanAlreadyExistsException() {
    }
}
