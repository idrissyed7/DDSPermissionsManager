package io.unityfoundation.dds.permissions.manager;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

@Singleton
public class FreemarkerConfiguration {
    private final Configuration configuration;

    public FreemarkerConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL resource = classloader.getResource("templates/");

        try {
            cfg.setDirectoryForTemplateLoading(new File(resource.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);

        // Wrap unchecked exceptions thrown during template processing into TemplateException-s:
        cfg.setWrapUncheckedExceptions(true);

        // Do not fall back to higher scopes when reading a null loop variable:
        cfg.setFallbackOnNullLoopVariable(false);

        this.configuration =  cfg;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
