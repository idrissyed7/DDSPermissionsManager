package io.unityfoundation.dds.permissions.manager;

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

    public static void main(String[] args) {
        Micronaut.run(RunApplication.class, args);
    }
}
