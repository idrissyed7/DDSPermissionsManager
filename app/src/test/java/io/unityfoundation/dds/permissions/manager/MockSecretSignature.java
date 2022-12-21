package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.security.token.jwt.signature.secret.SecretSignature;
import io.micronaut.security.token.jwt.signature.secret.SecretSignatureConfiguration;

@Replaces(SecretSignature.class)
@EachBean(SecretSignatureConfiguration.class)
public class MockSecretSignature extends SecretSignature {

    /**
     * @param config {@link SecretSignatureConfiguration} configuration
     */
    public MockSecretSignature(SecretSignatureConfiguration config) {
        super(config);
    }
}