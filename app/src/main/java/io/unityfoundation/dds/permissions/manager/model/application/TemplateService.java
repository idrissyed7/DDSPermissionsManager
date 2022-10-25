package io.unityfoundation.dds.permissions.manager.model.application;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.unityfoundation.dds.permissions.manager.FreemarkerConfiguration;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

@Singleton
public class TemplateService {
    private final FreemarkerConfiguration freemarkerConfiguration;

    public TemplateService(FreemarkerConfiguration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }

    public String mergeDataAndTemplate(Map<String, Object> dataModel) throws IOException, TemplateException {
        Template template = freemarkerConfiguration.getConfiguration().getTemplate("permissions.ftlx");

        String out;
        try(StringWriter stringWriter = new StringWriter()) {
            template.process(dataModel, stringWriter);
            out = stringWriter.toString();
        }

        return out;
    }
}
