package com.codereaper.Service;

import com.codereaper.Entity.User;

import java.util.Map;
import java.util.UUID;

public interface UserService {
    User requireById(UUID id);
    User upsertFromGithub(Map<String, Object> attrs, String token, String scope);
    public String DecryptAccessToken(User user);
}