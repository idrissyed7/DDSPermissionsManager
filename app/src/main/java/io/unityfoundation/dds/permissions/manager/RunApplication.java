package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Permissions Manager",
                version = "1.0",
                description = "Permissions Manager API"
        )
)
public class RunApplication {

    public static final String ENVIRONMENT_DEV_DATA = "dev-data";

    @ContextConfigurer
    public static class DefaultEnvironmentConfigurer implements ApplicationContextConfigurer {
        @Override
        public void configure(@NonNull ApplicationContextBuilder builder) {
            builder.defaultEnvironments(Environment.DEVELOPMENT, ENVIRONMENT_DEV_DATA);
        }
    }
    public static void main(String[] args) {
        Micronaut.run(RunApplication.class, args);
    }
}
