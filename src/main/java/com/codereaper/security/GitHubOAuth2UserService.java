package com.codereaper.security;

import com.codereaper.Entity.User;
//import com.codereaper.Entity.UserEntity;
import com.codereaper.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User githubUser = delegate.loadUser(request);

        String accessToken = request.getAccessToken().getTokenValue();
        String scopes = String.join(" ", request.getAccessToken().getScopes());

        User user = userService.upsertFromGithub(githubUser.getAttributes(), accessToken, scopes);

        Map<String, Object> attributes = new HashMap<>(githubUser.getAttributes());
        attributes.put("userId", user.getId().toString());

        String nameKey = request.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(githubUser.getAuthorities(), attributes, nameKey);
    }
}