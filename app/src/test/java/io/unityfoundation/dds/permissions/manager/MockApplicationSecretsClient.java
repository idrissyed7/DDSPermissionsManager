package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Replaces;
import io.unityfoundation.dds.permissions.manager.security.ApplicationSecretsClient;
import jakarta.inject.Singleton;

import java.util.Optional;

@Replaces(ApplicationSecretsClient.class)
@Singleton
public class MockApplicationSecretsClient extends ApplicationSecretsClient {

    public MockApplicationSecretsClient() {
    }

    public Optional<String> getIdentityCACert() {
        return Optional.of("identityCACert");
    }

    public Optional<String> getPermissionsCACert() {
        return Optional.of("permissionsCACert");
    }

    public Optional<String> getGovernanceFile() {
        return Optional.of("governanceFile");
    }
}