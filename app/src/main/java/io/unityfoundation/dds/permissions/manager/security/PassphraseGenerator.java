package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.security.token.jwt.encryption.EncryptionConfiguration;
import io.micronaut.security.token.jwt.encryption.secret.SecretEncryption;
import io.micronaut.security.token.jwt.encryption.secret.SecretEncryptionConfiguration;
import io.micronaut.security.token.jwt.encryption.secret.SecretEncryptionFactory;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Random;

@Singleton
@ConfigurationProperties("permissions-manager")
public class PassphraseGenerator {

    @MapFormat(transformation = MapFormat.MapTransformation.NESTED)
    private Map<String, Object> data;

    public String generatePassphrase(){
        int length = 16;
        if (data != null) {
            Map passphrase = (Map) data.get("passphrase");
            if (passphrase != null && passphrase.get("length") != null) {
                length = (int) passphrase.get("length");
            }
        }

        // see https://www.baeldung.com/java-random-string#java8-alphanumeric
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
