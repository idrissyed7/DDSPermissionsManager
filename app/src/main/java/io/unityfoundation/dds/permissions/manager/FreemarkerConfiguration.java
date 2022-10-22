package io.unityfoundation.dds.permissions.manager;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.io.scan.ClassPathResourceLoader;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

@Singleton
public class FreemarkerConfiguration {
    private final Configuration configuration;

    public FreemarkerConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);

        // Recommended but does not work - https://stackoverflow.com/a/53375338
//        ClassPathResourceLoader loader = new ResourceResolver().getLoader(ClassPathResourceLoader.class).get();
//        Optional<URL> resource = loader.getResource("templates/");

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL resource = classloader.getResource("templates/");

        try {
            cfg.setDirectoryForTemplateLoading(new File(resource.toURI()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
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
