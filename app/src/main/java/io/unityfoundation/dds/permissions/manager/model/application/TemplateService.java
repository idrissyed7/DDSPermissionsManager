// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.core.io.Writable;
import io.micronaut.views.ViewsRenderer;
import jakarta.inject.Singleton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Singleton
public class TemplateService {

    private final ViewsRenderer viewsRenderer;

    public TemplateService(ViewsRenderer viewsRenderer) {
        this.viewsRenderer = viewsRenderer;
    }

    public String mergeDataAndTemplate(Map<String, Object> dataModel) throws IOException {
        Writable permissions = viewsRenderer.render("permissions", dataModel, null);

        String out;
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream();) {
            permissions.writeTo(outputStream);
            out = outputStream.toString();
        }

        return out;
    }
}
