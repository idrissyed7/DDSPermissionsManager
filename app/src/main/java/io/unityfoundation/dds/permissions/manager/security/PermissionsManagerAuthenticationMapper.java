package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.security.authentication.AuthenticationFailureReason;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.oauth2.endpoint.authorization.state.State;
import io.micronaut.security.oauth2.endpoint.token.response.DefaultOpenIdAuthenticationMapper;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdAuthenticationMapper;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRole;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.*;

@Replaces(DefaultOpenIdAuthenticationMapper.class)
@Singleton
@Named("google")
public class PermissionsManagerAuthenticationMapper implements OpenIdAuthenticationMapper {

    private final UserService userService;
    private final GroupUserService groupUserService;

    public PermissionsManagerAuthenticationMapper(UserService userService, GroupUserService groupUserService) {
        this.userService = userService;
        this.groupUserService = groupUserService;
    }

    @Override
    public AuthenticationResponse createAuthenticationResponse(String providerName,
                                                               OpenIdTokenResponse tokenResponse,
                                                               OpenIdClaims openIdClaims,
                                                               State state) {
        return Optional.ofNullable(openIdClaims.getEmail())
                .flatMap(userService::getUserByEmail)
                .map(user -> AuthenticationResponse.success(openIdClaims.getEmail(),
                        rolesByUser(user),
                        userAttributes(openIdClaims, user)))
                .orElseGet(() -> AuthenticationResponse.failure(AuthenticationFailureReason.USER_NOT_FOUND));
    }

    private List<String> rolesByUser(User user) {
        return user.isAdmin() ? List.of(UserRole.ADMIN.toString()) : Collections.emptyList();
    }

    private HashMap<String, Object> userAttributes(OpenIdClaims openIdClaims, User user) {
        HashMap<String, Object> attributes = new HashMap<>();
        List<Map<String, Object>> permissions = groupUserService.getAllPermissionsPerGroupUserIsMemberOf(user.getId());
        attributes.put("name", openIdClaims.getName());
        attributes.put("permissionsByGroup", permissions);
        return attributes;
    }
}