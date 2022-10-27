package io.unityfoundation.dds.permissions.manager;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

@Factory
public class FreemarkerConfigurationFactory {
    @Singleton
    public Configuration getConfiguration() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL resource = classloader.getResource("templates/");

        try {
            configuration.setDirectoryForTemplateLoading(new File(resource.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);

        // Wrap unchecked exceptions thrown during template processing into TemplateException-s:
        configuration.setWrapUncheckedExceptions(true);

        // Do not fall back to higher scopes when reading a null loop variable:
        configuration.setFallbackOnNullLoopVariable(false);

        return configuration;
    }
}
