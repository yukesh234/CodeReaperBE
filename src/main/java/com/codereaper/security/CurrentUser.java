package com.codereaper.security;

import com.codereaper.Entity.User;
import com.codereaper.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserService userService;

    /** The logged-in user's internal id. Cheap: no database call. */
    public UUID id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken
                || !(auth.getPrincipal() instanceof OAuth2User principal)) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }
        Object userId = principal.getAttribute("userId");
        if (userId == null) {
            throw new AuthenticationCredentialsNotFoundException("Session has no userId");
        }
        return UUID.fromString(userId.toString());
    }


    public User get() {
        return userService.requireById(id());
    }
}