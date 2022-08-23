package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class SecurityUtil {

    private final SecurityService securityService;
    private final UserRepository userRepository;

    public SecurityUtil(SecurityService securityService, UserRepository userRepository) {
        this.securityService = securityService;
        this.userRepository = userRepository;
    }

    public boolean isCurrentUserAdmin() {
        Authentication authentication = securityService.getAuthentication().get();
        return Optional.of((Boolean) authentication.getAttributes().get("isAdmin")).orElse(false);
    }

    public Optional<User> getCurrentlyAuthenticatedUser() {
        Authentication authentication = securityService.getAuthentication().get();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail);
    }
}
