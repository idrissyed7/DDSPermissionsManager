// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Replaces;
import io.unityfoundation.dds.permissions.manager.security.ApplicationSecretsClient;
import jakarta.inject.Singleton;

import java.util.Optional;

@Replaces(ApplicationSecretsClient.class)
@Singleton
public class MockApplicationSecretsClient extends ApplicationSecretsClient {

    private String etag = "abc";
    private boolean hasCachedFileBeenUpdated = false;

    public MockApplicationSecretsClient() {
    }

    public Optional<String> getIdentityCACert() {
        return Optional.of("-----BEGIN CERTIFICATE-----\n" +
                "MIICgTCCAiagAwIBAgIJAKQ0JTu6DZZdMAoGCCqGSM49BAMCMIGSMQswCQYDVQQG\n" +
                "EwJVUzELMAkGA1UECAwCTU8xFDASBgNVBAcMC1NhaW50IExvdWlzMQ4wDAYDVQQK\n" +
                "DAVVTklUWTESMBAGA1UECwwJQ29tbVVOSVRZMRQwEgYDVQQDDAtJZGVudGl0eSBD\n" +
                "QTEmMCQGCSqGSIb3DQEJARYXaW5mb0B1bml0eWZvdW5kYXRpb24uaW8wHhcNMjIx\n" +
                "MDIwMTYwMjU3WhcNMjIxMTE5MTYwMjU3WjCBkjELMAkGA1UEBhMCVVMxCzAJBgNV\n" +
                "BAgMAk1PMRQwEgYDVQQHDAtTYWludCBMb3VpczEOMAwGA1UECgwFVU5JVFkxEjAQ\n" +
                "BgNVBAsMCUNvbW1VTklUWTEUMBIGA1UEAwwLSWRlbnRpdHkgQ0ExJjAkBgkqhkiG\n" +
                "9w0BCQEWF2luZm9AdW5pdHlmb3VuZGF0aW9uLmlvMFkwEwYHKoZIzj0CAQYIKoZI\n" +
                "zj0DAQcDQgAEMotkUz0ZUGv8lYvglXI5l6ArVs5PyWfz3civhogC4UuTZB9JyQOM\n" +
                "Xcvef04VJuhlvUJ4apUB2pW/exETApYKYqNjMGEwHQYDVR0OBBYEFAADg6XL4V94\n" +
                "czckFXsfuvBy1sJOMB8GA1UdIwQYMBaAFAADg6XL4V94czckFXsfuvBy1sJOMA8G\n" +
                "A1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgGGMAoGCCqGSM49BAMCA0kAMEYC\n" +
                "IQCtNp2FjbRmmpjAruZIu6L+zc+udvFS626HSugfxYGT0QIhAKKIZD0Q1OLbeFuf\n" +
                "9lAUsvd7k+g3XVrE1DJ3zoUh+NfQ\n" +
                "-----END CERTIFICATE-----");
    }

    public Optional<String> getPermissionsCACert() {
        return Optional.of("-----BEGIN CERTIFICATE-----\n" +
                "MIIChzCCAiygAwIBAgIJALs53aA9RNgXMAoGCCqGSM49BAMCMIGVMQswCQYDVQQG\n" +
                "EwJVUzELMAkGA1UECAwCTU8xFDASBgNVBAcMC1NhaW50IExvdWlzMQ4wDAYDVQQK\n" +
                "DAVVTklUWTESMBAGA1UECwwJQ29tbVVOSVRZMRcwFQYDVQQDDA5QZXJtaXNzaW9u\n" +
                "cyBDQTEmMCQGCSqGSIb3DQEJARYXaW5mb0B1bml0eWZvdW5kYXRpb24uaW8wHhcN\n" +
                "MjIxMDIwMTYwMzQ3WhcNMjIxMTE5MTYwMzQ3WjCBlTELMAkGA1UEBhMCVVMxCzAJ\n" +
                "BgNVBAgMAk1PMRQwEgYDVQQHDAtTYWludCBMb3VpczEOMAwGA1UECgwFVU5JVFkx\n" +
                "EjAQBgNVBAsMCUNvbW1VTklUWTEXMBUGA1UEAwwOUGVybWlzc2lvbnMgQ0ExJjAk\n" +
                "BgkqhkiG9w0BCQEWF2luZm9AdW5pdHlmb3VuZGF0aW9uLmlvMFkwEwYHKoZIzj0C\n" +
                "AQYIKoZIzj0DAQcDQgAETpQ3PmzztDkjocQ4jDXDmJSLKNTywfhBj+TaMRMj1llR\n" +
                "zzyyg84CAxvOo6aXurB7DP2mOLd3e+JcCnxyIYIjzaNjMGEwHQYDVR0OBBYEFMYW\n" +
                "gviUzxL5RbpFMYDY4TgcSVQXMB8GA1UdIwQYMBaAFMYWgviUzxL5RbpFMYDY4Tgc\n" +
                "SVQXMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgGGMAoGCCqGSM49BAMC\n" +
                "A0kAMEYCIQCzNAsDtL2g3M91hl5i4EUmFtpL4KFnoQ6XrxuatOo63wIhALZhDjMy\n" +
                "TsRZo3Lrd0CARNDlnrUNQwgblVJCtmjCK5V4\n" +
                "-----END CERTIFICATE-----");
    }

    public Optional<String> getGovernanceFile() {
        return Optional.of("MIME-Version: 1.0\n" +
                "Content-Type: multipart/signed; protocol=\"application/x-pkcs7-signature\"; micalg=\"sha1\"; boundary=\"----E7CBBA7468B3989AB3841DB52EB8FBDC\"\n" +
                "\n" +
                "This is an S/MIME signed message\n" +
                "\n" +
                "------E7CBBA7468B3989AB3841DB52EB8FBDC\n" +
                "Content-Type: text/plain\n" +
                "\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!--\n" +
                "  Illustrates DDS Security can be used to protect access to a DDS Domain.\n" +
                "  Only applications that can authenticate and have the proper permissions can\n" +
                "  join the Domain. Others cannot publish nor subscribe.\n" +
                "-->\n" +
                "<dds xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.omg.org/spec/DDS-SECURITY/20170901/omg_shared_ca_permissions.xsd\">\n" +
                "  <domain_access_rules>\n" +
                "    <!--\n" +
                "      Domain 0 is a \"protected domain.\" That is, only applications that\n" +
                "      can authenticate and have proper permissions can join it.\n" +
                "    -->\n" +
                "    <domain_rule>\n" +
                "      <domains>\n" +
                "        <id>0</id>\n" +
                "      </domains>\n" +
                "      <allow_unauthenticated_participants>false</allow_unauthenticated_participants>\n" +
                "      <enable_join_access_control>true</enable_join_access_control>\n" +
                "      <discovery_protection_kind>ENCRYPT</discovery_protection_kind>\n" +
                "      <liveliness_protection_kind>ENCRYPT</liveliness_protection_kind>\n" +
                "      <rtps_protection_kind>ENCRYPT</rtps_protection_kind>\n" +
                "      <topic_access_rules>\n" +
                "        <topic_rule>\n" +
                "          <topic_expression>B*</topic_expression>\n" +
                "          <enable_discovery_protection>true</enable_discovery_protection>\n" +
                "          <enable_liveliness_protection>true</enable_liveliness_protection>\n" +
                "          <enable_read_access_control>false</enable_read_access_control>\n" +
                "          <enable_write_access_control>true</enable_write_access_control>\n" +
                "          <metadata_protection_kind>ENCRYPT</metadata_protection_kind>\n" +
                "          <data_protection_kind>NONE</data_protection_kind>\n" +
                "        </topic_rule>\n" +
                "        <topic_rule>\n" +
                "          <topic_expression>C*</topic_expression>\n" +
                "          <enable_discovery_protection>true</enable_discovery_protection>\n" +
                "          <enable_liveliness_protection>true</enable_liveliness_protection>\n" +
                "          <enable_read_access_control>true</enable_read_access_control>\n" +
                "          <enable_write_access_control>true</enable_write_access_control>\n" +
                "          <metadata_protection_kind>ENCRYPT</metadata_protection_kind>\n" +
                "          <data_protection_kind>NONE</data_protection_kind>\n" +
                "        </topic_rule>\n" +
                "      </topic_access_rules>\n" +
                "    </domain_rule>\n" +
                "  </domain_access_rules>\n" +
                "</dds>\n" +
                "\n" +
                "------E7CBBA7468B3989AB3841DB52EB8FBDC\n" +
                "Content-Type: application/x-pkcs7-signature; name=\"smime.p7s\"\n" +
                "Content-Transfer-Encoding: base64\n" +
                "Content-Disposition: attachment; filename=\"smime.p7s\"\n" +
                "\n" +
                "MIIE2gYJKoZIhvcNAQcCoIIEyzCCBMcCAQExCzAJBgUrDgMCGgUAMAsGCSqGSIb3\n" +
                "DQEHAaCCAoswggKHMIICLKADAgECAgkAuzndoD1E2BcwCgYIKoZIzj0EAwIwgZUx\n" +
                "CzAJBgNVBAYTAlVTMQswCQYDVQQIDAJNTzEUMBIGA1UEBwwLU2FpbnQgTG91aXMx\n" +
                "DjAMBgNVBAoMBVVOSVRZMRIwEAYDVQQLDAlDb21tVU5JVFkxFzAVBgNVBAMMDlBl\n" +
                "cm1pc3Npb25zIENBMSYwJAYJKoZIhvcNAQkBFhdpbmZvQHVuaXR5Zm91bmRhdGlv\n" +
                "bi5pbzAeFw0yMjEwMjAxNjAzNDdaFw0yMjExMTkxNjAzNDdaMIGVMQswCQYDVQQG\n" +
                "EwJVUzELMAkGA1UECAwCTU8xFDASBgNVBAcMC1NhaW50IExvdWlzMQ4wDAYDVQQK\n" +
                "DAVVTklUWTESMBAGA1UECwwJQ29tbVVOSVRZMRcwFQYDVQQDDA5QZXJtaXNzaW9u\n" +
                "cyBDQTEmMCQGCSqGSIb3DQEJARYXaW5mb0B1bml0eWZvdW5kYXRpb24uaW8wWTAT\n" +
                "BgcqhkjOPQIBBggqhkjOPQMBBwNCAAROlDc+bPO0OSOhxDiMNcOYlIso1PLB+EGP\n" +
                "5NoxEyPWWVHPPLKDzgIDG86jppe6sHsM/aY4t3d74lwKfHIhgiPNo2MwYTAdBgNV\n" +
                "HQ4EFgQUxhaC+JTPEvlFukUxgNjhOBxJVBcwHwYDVR0jBBgwFoAUxhaC+JTPEvlF\n" +
                "ukUxgNjhOBxJVBcwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAYYwCgYI\n" +
                "KoZIzj0EAwIDSQAwRgIhALM0CwO0vaDcz3WGXmLgRSYW2kvgoWehDpevG5q06jrf\n" +
                "AiEAtmEOMzJOxFmjcut3QIBE0OWetQ1DCBuVUkK2aMIrlXgxggIXMIICEwIBATCB\n" +
                "ozCBlTELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAk1PMRQwEgYDVQQHDAtTYWludCBM\n" +
                "b3VpczEOMAwGA1UECgwFVU5JVFkxEjAQBgNVBAsMCUNvbW1VTklUWTEXMBUGA1UE\n" +
                "AwwOUGVybWlzc2lvbnMgQ0ExJjAkBgkqhkiG9w0BCQEWF2luZm9AdW5pdHlmb3Vu\n" +
                "ZGF0aW9uLmlvAgkAuzndoD1E2BcwCQYFKw4DAhoFAKCCAQcwGAYJKoZIhvcNAQkD\n" +
                "MQsGCSqGSIb3DQEHATAcBgkqhkiG9w0BCQUxDxcNMjIxMDIwMTYwNDE5WjAjBgkq\n" +
                "hkiG9w0BCQQxFgQUJPFYFIoOlpQ91q+BZSPGJZdoCq8wgacGCSqGSIb3DQEJDzGB\n" +
                "mTCBljALBglghkgBZQMEASowCAYGKoUDAgIJMAoGCCqFAwcBAQICMAoGCCqFAwcB\n" +
                "AQIDMAgGBiqFAwICFTALBglghkgBZQMEARYwCwYJYIZIAWUDBAECMAoGCCqGSIb3\n" +
                "DQMHMA4GCCqGSIb3DQMCAgIAgDANBggqhkiG9w0DAgIBQDAHBgUrDgMCBzANBggq\n" +
                "hkiG9w0DAgIBKDAJBgcqhkjOPQQBBEcwRQIgFusiTtqdVhanba5ezc++SFT4VQY7\n" +
                "v1bEzsC03JCRauYCIQCKqjWLJDrJpwd7ruWSFpQ6w/iyqN13BlWdnrn2KywpEw==\n" +
                "\n" +
                "------E7CBBA7468B3989AB3841DB52EB8FBDC--\n" +
                "\n");
    }

    public Optional<String> getIdentityCAKey() {
        return Optional.of("-----BEGIN EC PRIVATE KEY-----\n" +
                "MHcCAQEEIHSeQ18M8nQB/rPCepS4sA/qEh1FCHfZ4dDvkR4S8QtZoAoGCCqGSM49\n" +
                "AwEHoUQDQgAEMotkUz0ZUGv8lYvglXI5l6ArVs5PyWfz3civhogC4UuTZB9JyQOM\n" +
                "Xcvef04VJuhlvUJ4apUB2pW/exETApYKYg==\n" +
                "-----END EC PRIVATE KEY-----");
    }

    public Optional<String> getPermissionsCAKey() {
        return Optional.of("-----BEGIN EC PRIVATE KEY-----\n" +
                "MHcCAQEEIDTKJ3wSi26GSg7M9he8b1+1hxa1cKq/Rlu9tLT7COFtoAoGCCqGSM49\n" +
                "AwEHoUQDQgAEgUJtwTNZffFeUf0deYHps5Opvz5hCNcoLdUrloMRceC6FOiJqq6V\n" +
                "OgxxVU9lwr9fJuAaZm0uQpyOYvgAqBLw5w==\n" +
                "-----END EC PRIVATE KEY-----\n");
    }

    public boolean hasCachedFileBeenUpdated(String file) {
        return this.hasCachedFileBeenUpdated;
    }

    @Override
    public String getCorrespondingEtag(String file) {
        return this.etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public void setHasCachedFileBeenUpdated(boolean hasCachedFileBeenUpdated) {
        this.hasCachedFileBeenUpdated = hasCachedFileBeenUpdated;
    }
}