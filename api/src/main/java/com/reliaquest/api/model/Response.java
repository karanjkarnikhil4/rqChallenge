package com.reliaquest.api.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public record Response<T>(T data, Status status, String message) {

    @Getter
    public enum Status {
        HANDLED("Successfully processed request."),
        ERROR("Failed to process request.");

        @JsonValue
        private final String value;

        Status(String value) {
            this.value = value;
        }
    }
}
