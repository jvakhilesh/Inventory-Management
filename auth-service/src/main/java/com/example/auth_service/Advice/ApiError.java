package com.example.auth_service.Advice;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import org.springframework.http.HttpStatus;

@Data
public class ApiError {

    @JsonFormat(pattern = "HH:mm:ss dd-MM-yyyy")
    private LocalDateTime timeStamp;

    private HttpStatus status;
    private String errors;

    public ApiError() {
        this.timeStamp = LocalDateTime.now();
    }

    public ApiError(HttpStatus status, String errors) {
        this();
        this.status = status;
        this.errors = errors;
    }

    public String getMessage() {
        return errors;
    }
}
