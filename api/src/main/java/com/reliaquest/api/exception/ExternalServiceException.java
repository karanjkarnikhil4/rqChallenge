package com.reliaquest.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ExternalServiceException extends RuntimeException {
    private final HttpStatusCode status;

    public ExternalServiceException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }
}
