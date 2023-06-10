package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.encoders.Hex;

import javax.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Singleton
public class BCryptPasswordEncoderService {

    @Property(name = "permissions-manager.application.passphrase.salt")
    protected String salt;

    private Argon2BytesGenerator getGenerator() {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt.getBytes(StandardCharsets.UTF_8));

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());
        return generator;
    }

    public String encode(@NotBlank @NonNull String rawPassword) {
        byte[] result = new byte[32];
        getGenerator().generateBytes(rawPassword.getBytes(StandardCharsets.UTF_8), result);

        return Hex.toHexString(result);
    }

    public boolean matches(@NotBlank @NonNull String rawPassword,
                    @NotBlank @NonNull String encodedPassword) {
        byte[] result = new byte[32];
        getGenerator().generateBytes(rawPassword.getBytes(StandardCharsets.UTF_8), result);

        return Arrays.equals(result, Hex.decode(encodedPassword));
    }
}
