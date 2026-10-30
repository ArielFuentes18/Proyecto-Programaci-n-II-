package com.sistematickets.Service;

public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.status = -1;
    }

    public int getStatus() { return status; }
}
