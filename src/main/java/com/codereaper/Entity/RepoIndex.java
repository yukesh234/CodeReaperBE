package com.codereaper.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repo_index",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "github_repo_id"}))
@Getter @Setter @NoArgsConstructor
public class RepoIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private long githubRepoId;

    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndexStatus status = IndexStatus.NONE;

    private int filesProcessed;
    private int filesTotal;
    private int chunkCount;

    @Column(length = 1000)
    private String errorMessage;

    private String indexedCommitSha;
    private Instant lastIndexedAt;
    private Instant updatedAt;

    public void markIndexing() {
        status = IndexStatus.INDEXING;
        filesProcessed = 0;
        filesTotal = 0;
        chunkCount = 0;
        errorMessage = null;
        updatedAt = Instant.now();
    }
}