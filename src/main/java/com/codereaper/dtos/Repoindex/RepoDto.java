package com.codereaper.dtos.Repoindex;

public record RepoDto(
        long id, String fullName, String description, String language,
        boolean isPrivate, String defaultBranch, String indexStatus,
        int filesProcessed, int filesTotal, int chunkCount, String errorMessage) {}