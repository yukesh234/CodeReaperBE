package com.codereaper.controller;

import com.codereaper.Entity.User;
import com.codereaper.dtos.userdto.userResponsedto;
import com.codereaper.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final CurrentUser currentUser;


    @GetMapping("/login-url")
    public Map<String,String> loginUrl(){
        return Map.of("url","/oauth2/authorization/github");
    }

    @GetMapping("/me")
    public ResponseEntity<userResponsedto>me(){
        User user = currentUser.get();
        return ResponseEntity.ok(new userResponsedto(
                user.getId(),
                user.getGithubid(),
                user.getGithubname(),
                user.getDisplayname(),
                user.getAvatar()
        ));
    }

}
