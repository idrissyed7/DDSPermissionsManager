package io.unityfoundation.dds.permissions.manager.exception;

import io.micronaut.http.HttpStatus;

public class DPMException extends RuntimeException {
    private String responseStatusCode;
    private HttpStatus httpStatus;

    public DPMException(String responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
    }

    public DPMException(String responseStatusCode, HttpStatus httpStatus) {
        this.responseStatusCode = responseStatusCode;
        this.httpStatus = httpStatus;
    }

    public String getResponseStatusCode() {
        return responseStatusCode;
    }

    public void setResponseStatusCode(String responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
