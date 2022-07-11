package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;

import javax.validation.constraints.NotBlank;

@Requires(property = HostedDomainConfigurationProperties.PREFIX + ".url")
@ConfigurationProperties(HostedDomainConfigurationProperties.PREFIX)
public class HostedDomainConfigurationProperties implements HostedDomain {
    public static final String PREFIX = "hosted-domain";

    @NonNull
    @NotBlank
    private String url;

    public void setUrl(@NonNull String url) {
        this.url = url;
    }

    @Override
    @NonNull
    public String getUrl() {
        return url;
    }
}
