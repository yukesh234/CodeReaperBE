package com.codereaper.Service.Impl;

import com.codereaper.Entity.User;
import com.codereaper.Repo.UserRepo;
import com.codereaper.Service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final TextEncryptor textEncryptor;



    @Override
    public String DecryptAccessToken(User user) {
        return textEncryptor.decrypt(user.getAccessToken());
    }

    @Override
    @Transactional(readOnly = true)
    public User requireById(UUID id) {
        return userRepo.findById(id).orElseThrow(()-> new IllegalArgumentException("User not found"));
    }

    @Override
    @Transactional
    public User upsertFromGithub(Map<String, Object> attrs, String token, String scope) {
        long githubId = ((Number) attrs.get("id")).longValue();
        String login = (String) attrs.get("login");
        String name = (String) attrs.get("name");

        User user = (User) userRepo.findByGithubid(githubId).orElseGet(User::new);
        user.setGithubid(githubId);
        user.setGithubname(login);
        user.setDisplayname(name != null ? name : login);
        user.setAvatar((String) attrs.get("avatar_url"));
        user.setAccessToken(token);   // was encryptedToken
        user.setTokenScope(scope);
        return userRepo.save(user);
    }


}
