package com.example.demo.exceptions;

public class PlanNotFoundException extends RuntimeException {

    private String message;

    public PlanNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    public PlanNotFoundException() {
    }
}
