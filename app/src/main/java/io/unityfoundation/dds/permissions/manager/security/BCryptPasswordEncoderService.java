package io.unityfoundation.dds.permissions.manager.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;
import javax.validation.constraints.NotBlank;

@Singleton
public class BCryptPasswordEncoderService {

    public String encode(@NotBlank @NonNull String rawPassword) {
        return BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray());
    }

    public boolean matches(@NotBlank @NonNull String rawPassword,
                    @NotBlank @NonNull String encodedPassword) {
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), encodedPassword.toCharArray()).verified;
    }
}
