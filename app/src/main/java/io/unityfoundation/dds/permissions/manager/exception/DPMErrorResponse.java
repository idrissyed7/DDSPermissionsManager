// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.exception;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class DPMErrorResponse {

    private String id;
    private String code;

    public DPMErrorResponse() {
    }

    public DPMErrorResponse(String id, String code) {
        this.id = id;
        this.code = code;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
