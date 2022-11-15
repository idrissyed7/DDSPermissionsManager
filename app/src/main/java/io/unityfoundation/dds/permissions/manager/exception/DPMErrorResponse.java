package io.unityfoundation.dds.permissions.manager.exception;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class DPMErrorResponse {

    private String code;

    public DPMErrorResponse() {
    }

    public DPMErrorResponse(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
