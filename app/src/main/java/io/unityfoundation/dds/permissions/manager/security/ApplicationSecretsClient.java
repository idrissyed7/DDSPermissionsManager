package io.unityfoundation.dds.permissions.manager.security;

import com.google.cloud.secretmanager.v1.*;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Optional;

@Singleton
public class ApplicationSecretsClient {

    @Property(name = "gcp.project-id")
    protected Optional<String> projectOptional;
    @Property(name = "gcp.credentials.enabled")
    protected Optional<Boolean> enabled;
    private SecretManagerServiceClient client;
    private String identityCACert;
    private String permissionsCACert;
    private String governanceFile;

    public ApplicationSecretsClient() {
    }

    @EventListener
    public void onStartup(StartupEvent event) throws IOException {

        if (projectOptional.isPresent() && enabled.isPresent() && enabled.get()) {
            this.client = SecretManagerServiceClient.create();

            String project = projectOptional.get();

            try {
                this.identityCACert =  getAccessSecretVersionResponse(project, "identity_ca_pem")
                        .getPayload().getData().toStringUtf8();

                this.permissionsCACert = getAccessSecretVersionResponse(project, "permissions_ca_pem")
                        .getPayload().getData().toStringUtf8();

                this.governanceFile = getAccessSecretVersionResponse(project, "governance_xml_p7s")
                        .getPayload().getData().toStringUtf8();

            } catch (Exception e) {
                // all or nothing
                this.identityCACert = null;
                this.permissionsCACert = null;
                this.governanceFile = null;
            }
        }
    }

    private AccessSecretVersionResponse getAccessSecretVersionResponse(String project, String file) {
        AccessSecretVersionResponse response = client.accessSecretVersion(AccessSecretVersionRequest
                .newBuilder()
                .setName(SecretVersionName.of(project, file, "latest").toString())
                .build());
        return response;
    }

    public Optional<String> getIdentityCACert() {
        return Optional.ofNullable(identityCACert);
    }

    public Optional<String> getPermissionsCACert() {
        return Optional.ofNullable(permissionsCACert);
    }

    public Optional<String> getGovernanceFile() {
        return Optional.ofNullable(governanceFile);
    }

    public Optional<String> getIdentityCAKey() {
        if (projectOptional.isPresent() && enabled.isPresent() && enabled.get()) {
            return Optional.ofNullable(getAccessSecretVersionResponse(projectOptional.get(), "identity_ca_key_pem")
                    .getPayload().getData().toStringUtf8());
        }

        return Optional.empty();
    }
}