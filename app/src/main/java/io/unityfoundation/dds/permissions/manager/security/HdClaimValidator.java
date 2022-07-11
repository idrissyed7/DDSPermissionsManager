package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.token.jwt.generator.claims.JwtClaims;
import io.micronaut.security.token.jwt.validator.GenericJwtClaimsValidator;
import jakarta.inject.Singleton;

@Requires(beans = HostedDomain.class)
@Singleton
public class HdClaimValidator implements GenericJwtClaimsValidator {

    private final HostedDomain hostedDomain;

    public HdClaimValidator(HostedDomain hostedDomain) {
        this.hostedDomain = hostedDomain;
    }

    @Override
    public boolean validate(@NonNull JwtClaims claims, @Nullable HttpRequest<?> request) {
        Object value = claims.get(HostedDomain.CLAIM_HD);
        if (value == null) {
            return false;
        }
        return value.toString().equals(hostedDomain.getUrl());
    }
}
