package com.codereaper.dtos.userdto;

import java.util.UUID;

public record userResponsedto(
        UUID id,
        long githubid,
        String githubname,
        String displayname,
        String avatar
) { }
