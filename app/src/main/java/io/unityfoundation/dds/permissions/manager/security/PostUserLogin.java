package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.event.LoginSuccessfulEvent;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;

@Singleton
public class PostUserLogin {

    @Inject UserService userService;

    @EventListener
    public void loginSuccessful(LoginSuccessfulEvent event) {
        Authentication source = (Authentication) event.getSource();
        Map<String, Object> authAttributes = source.getAttributes();
        String email = (String) authAttributes.get("email");
        if (email != null && !userService.userExistsByEmail(email)) {
            String givenName = (String) authAttributes.get("given_name");
            String familyName = (String) authAttributes.get("family_name");
            User user = new User(givenName, familyName, email);
            userService.save(user);
        }
    }
}
