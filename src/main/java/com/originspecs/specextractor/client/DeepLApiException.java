package com.originspecs.specextractor.client;

import lombok.Getter;

@Getter
public class DeepLApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public DeepLApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public DeepLApiException(String message, int statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    @Override
    public String toString() {
        return "DeepLApiException{" +
                "message='" + getMessage() + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}
