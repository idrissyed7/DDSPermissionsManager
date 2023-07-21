package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.security.utils.SecurityService;
import io.unityfoundation.dds.permissions.manager.model.user.UserRole;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SecurityServiceReplacement implements SecurityService {

    private ServerAuthentication serverAuthentication;

    @PostConstruct
    void postConstruct() {
        serverAuthentication = new ServerAuthentication("montesm@test.test.com", null, null);
    }

    @Override
    public Optional<String> username() {
        return Optional.empty();
    }

    @Override
    public Optional<Authentication> getAuthentication() {
        return Optional.of(serverAuthentication);
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public boolean hasRole(String role) {
        return false;
    }

    public void setServerAuthentication(ServerAuthentication serverAuthentication) {
        this.serverAuthentication = serverAuthentication;
    }
}
