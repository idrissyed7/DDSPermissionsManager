package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.model.user.UserRole;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class SecurityUtil {

    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public SecurityUtil(SecurityService securityService, UserRepository userRepository, ApplicationRepository applicationRepository) {
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    public boolean isCurrentUserAdmin() {
        Authentication authentication = securityService.getAuthentication().get();
        return authentication.getRoles().contains(UserRole.ADMIN.toString());
    }

    public Optional<User> getCurrentlyAuthenticatedUser() {
        Authentication authentication = securityService.getAuthentication().get();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail);
    }

    public Optional<Application> getCurrentlyAuthenticatedApplication() {
        Authentication authentication = securityService.getAuthentication().get();
        String applicationId = authentication.getName();
        return applicationRepository.findById(Long.valueOf(applicationId));
    }
}
