package com.codereaper.Repo;

import com.codereaper.Entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepo extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByRepoIndexIdAndUserIdOrderByCreatedAtDesc(UUID repoIndexId, UUID userId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("delete from ChatMessage m where m.repoIndexId = :repoIndexId and m.userId = :userId")
    void deleteAllFor(@Param("repoIndexId") UUID repoIndexId, @Param("userId") UUID userId);
}