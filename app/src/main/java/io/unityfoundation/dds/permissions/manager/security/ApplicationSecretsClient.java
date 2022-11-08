package io.unityfoundation.dds.permissions.manager.security;

import com.google.cloud.secretmanager.v1.*;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

@Singleton
public class ApplicationSecretsClient {

    public static final String IDENTITY_CA_CERT = "identity_ca_pem";
    public static final String IDENTITY_CA_KEY = "identity_ca_key_pem";
    public static final String PERMISSIONS_CA_CERT = "permissions_ca_pem";
    public static final String PERMISSIONS_CA_KEY = "permissions_ca_key_pem";
    public static final String GOVERNANCE_FILE = "governance_xml_p7s";

    @Property(name = "gcp.project-id")
    protected String project;
    @Property(name = "gcp.credentials.enabled")
    protected Boolean enabled;
    private String identityCACert;
    private String identityCACertETag;
    private String identityCAKey;
    private String identityCAKeyETag;
    private String permissionsCACert;
    private String permissionsCACertETag;
    private String permissionsCAKey;
    private String permissionsCAKeyETag;
    private String governanceFile;
    private String governanceFileETag;

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationSecretsClient.class);

    public ApplicationSecretsClient() {
    }

    @EventListener
    public void onStartup(StartupEvent event) throws IOException {

        if (project != null && enabled != null && enabled) {
            try {
                SecretManagerServiceClient client = SecretManagerServiceClient.create();
                this.identityCACert =  getLatestSecret(client, project, IDENTITY_CA_CERT);
                this.identityCACertETag =  getLatestSecretETag(client, project, IDENTITY_CA_CERT);
                this.identityCAKey =  getLatestSecret(client, project, IDENTITY_CA_KEY);
                this.identityCAKeyETag =  getLatestSecretETag(client, project, IDENTITY_CA_KEY);
                this.permissionsCACert = getLatestSecret(client, project, PERMISSIONS_CA_CERT);
                this.permissionsCACertETag = getLatestSecretETag(client, project, PERMISSIONS_CA_CERT);
                this.permissionsCAKey = getLatestSecret(client, project, PERMISSIONS_CA_KEY);
                this.permissionsCAKeyETag = getLatestSecretETag(client, project, PERMISSIONS_CA_KEY);
                this.governanceFile = getLatestSecret(client, project, GOVERNANCE_FILE);
                this.governanceFileETag = getLatestSecretETag(client, project, GOVERNANCE_FILE);
                client.close();
            } catch (Exception e) {
                LOG.error("Could not get secrets from GCP: " + e.getMessage());
                // all or nothing
                this.identityCACert = null;
                this.identityCACertETag = null;
                this.identityCAKey = null;
                this.identityCAKeyETag = null;
                this.permissionsCACert = null;
                this.permissionsCACertETag = null;
                this.permissionsCAKey = null;
                this.permissionsCAKeyETag = null;
                this.governanceFile = null;
                this.governanceFileETag = null;
            }
        }
    }

    private String getLatestSecret(SecretManagerServiceClient client, String project, String file) {
        AccessSecretVersionResponse response = client.accessSecretVersion(AccessSecretVersionRequest
                .newBuilder()
                .setName(SecretVersionName.of(project, file, "latest").toString())
                .build());
        return response.getPayload().getData().toStringUtf8();
    }

    public String getLatestSecretETag(SecretManagerServiceClient client, String project, String file) {
        SecretVersion secretVersion = client.getSecretVersion(GetSecretVersionRequest
                .newBuilder()
                .setName(SecretVersionName.of(project, file, "latest").toString())
                .build());
        return secretVersion.getEtag();
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
        return Optional.ofNullable(identityCAKey);
    }

    public Optional<String> getPermissionsCAKey() {
        return Optional.ofNullable(permissionsCAKey);
    }

    public boolean hasCachedFileBeenUpdated(String file) {
        String etag = getCorrespondingEtag(file);

        try(SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            String latestSecretETag = getLatestSecretETag(client, project, file);
            if (!etag.contentEquals(latestSecretETag)) {
                setFileAndETag(file, etag, getLatestSecret(client, project, file));
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    private void setFileAndETag(String file, String etag, String latestSecret) {
        switch(file) {
            case IDENTITY_CA_CERT:
                this.identityCACert = latestSecret;
                this.identityCACertETag = etag;
            case IDENTITY_CA_KEY:
                this.identityCAKey = latestSecret;
                this.identityCAKeyETag = etag;
            case PERMISSIONS_CA_CERT:
                this.permissionsCACert = latestSecret;
                this.permissionsCACertETag = etag;
            case PERMISSIONS_CA_KEY:
                this.permissionsCAKey = latestSecret;
                this.permissionsCAKeyETag = etag;
            case GOVERNANCE_FILE:
                this.governanceFile = latestSecret;
                this.governanceFileETag = etag;
        }
    }

    public String getCorrespondingEtag(String file) {
        switch(file) {
            case IDENTITY_CA_CERT:
                return this.identityCACertETag;
            case IDENTITY_CA_KEY:
                return this.identityCAKeyETag;
            case PERMISSIONS_CA_CERT:
                return this.permissionsCACertETag;
            case PERMISSIONS_CA_KEY:
                return this.permissionsCAKeyETag;
            case GOVERNANCE_FILE:
                return this.governanceFileETag;
        }
        return null;
    }
}
