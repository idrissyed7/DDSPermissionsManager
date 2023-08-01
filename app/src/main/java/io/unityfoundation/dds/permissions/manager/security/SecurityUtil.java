// Copyright 2023 DDS Permissions Manager Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.http.HttpStatus;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import io.unityfoundation.dds.permissions.manager.ResponseStatusCodes;
import io.unityfoundation.dds.permissions.manager.exception.DPMException;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
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
        Optional<User> currentlyAuthenticatedUser = getCurrentlyAuthenticatedUser();
        if (currentlyAuthenticatedUser.isEmpty()) {
            throw new DPMException(ResponseStatusCodes.USER_NOT_FOUND, HttpStatus.UNAUTHORIZED);
        }
        return currentlyAuthenticatedUser.get().isAdmin();
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
