package io.unityfoundation.dds.permissions.manager.security;

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class ApplicationSecretsClient {

    @Property(name = "permissions.manager.gcp.project")
    protected Optional<String> projectOptional;
    private final SecretManagerServiceClient client;
    private String identityCACert;
    private String permissionsCACert;
    private String governanceFile;

    public ApplicationSecretsClient(SecretManagerServiceClient googleSecretManagerClient) {
        this.client = googleSecretManagerClient;
    }

    @EventListener
    public void onStartup(StartupEvent event) {

        if (projectOptional.isPresent()) {
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
}