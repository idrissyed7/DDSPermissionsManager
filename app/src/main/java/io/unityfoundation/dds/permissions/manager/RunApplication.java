package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.bouncycastle.mail.smime.handlers.multipart_signed;
import org.bouncycastle.mail.smime.handlers.pkcs7_mime;
import org.bouncycastle.mail.smime.handlers.pkcs7_signature;


@TypeHint(
        value = {
                pkcs7_mime.class,
                pkcs7_signature.class,
                multipart_signed.class
        }
)
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
