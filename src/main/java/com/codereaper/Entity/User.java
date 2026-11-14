package com.codereaper.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private long githubid;
    @Column(unique = true, nullable = false)
    private String githubname;
    private String displayname;
    private String avatar;
    private String accessToken;
    private String tokenScope;
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if(this.createdAt == null) {
        createdAt = Instant.now();
        }
    }
}
