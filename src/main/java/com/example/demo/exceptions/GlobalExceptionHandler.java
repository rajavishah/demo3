package com.example.demo.exceptions;

import lombok.Data;

@Data
public class GlobalExceptionHandler {
    String msg;
    String error;
    int statusCode;
    Object data;

    public GlobalExceptionHandler(String msg, String err, int statusCode, Object data) {
        this.msg = msg;
        this.error = err;
        this.statusCode = statusCode;
        this.data = data;
    }

    @Override
    public String toString() {
        return "GlobalExceptionHandler{" +
                "msg='" + msg + '\'' +
                ", error='" + error + '\'' +
                ", statusCode=" + statusCode +
                ", data=" + data +
                '}';
    }
}
