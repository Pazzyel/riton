package com.riton.exception;

public class BusinessException extends RuntimeException {

    public BusinessException() {
    }

    public BusinessException(String msg) {
        super(msg);
    }

}