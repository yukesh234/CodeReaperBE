package com.codereaper.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_message", indexes = @Index(columnList = "repo_index_id,created_at"))
@Getter @Setter @NoArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID repoIndexId;
    @Column(nullable = false, length = 16) private String role;            // "user" or "assistant"
    @Column(nullable = false, columnDefinition = "text") private String content;
    @Column(columnDefinition = "text") private String sources;             // "path<TAB>line" per row
    @Column(nullable = false) private Instant createdAt;
}