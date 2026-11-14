package com.codereaper.Repo;

import com.codereaper.Entity.IndexStatus;
import com.codereaper.Entity.RepoIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepoIndexRepo extends JpaRepository<RepoIndex, UUID> {

    List<RepoIndex> findByUserId(UUID userId);

    Optional<RepoIndex> findByUserIdAndGithubRepoId(UUID userId, long githubRepoId);

    /** Returns 1 if this call flipped the row to INDEXING, 0 if it was already indexing. */
    @Modifying
    @Transactional
    @Query("""
        update RepoIndex r set r.status = com.codereaper.Entity.IndexStatus.INDEXING,
          r.filesProcessed = 0, r.filesTotal = 0, r.chunkCount = 0,
          r.errorMessage = null, r.updatedAt = :now
        where r.id = :id and r.status <> com.codereaper.Entity.IndexStatus.INDEXING""")
    int claimForIndexing(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("update RepoIndex r set r.filesTotal = :total, r.updatedAt = :now where r.id = :id")
    void updateTotal(@Param("id") UUID id, @Param("total") int total, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("update RepoIndex r set r.filesProcessed = :processed, r.chunkCount = :chunks, r.updatedAt = :now where r.id = :id")
    void updateProgress(@Param("id") UUID id, @Param("processed") int processed,
                        @Param("chunks") int chunks, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("""
        update RepoIndex r set r.status = com.codereaper.Entity.IndexStatus.READY,
          r.indexedCommitSha = :sha, r.filesProcessed = :processed, r.chunkCount = :chunks,
          r.lastIndexedAt = :now, r.updatedAt = :now, r.errorMessage = null
        where r.id = :id""")
    void markReady(@Param("id") UUID id, @Param("sha") String sha,
                   @Param("processed") int processed, @Param("chunks") int chunks,
                   @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("""
        update RepoIndex r set r.status = com.codereaper.Entity.IndexStatus.FAILED,
          r.errorMessage = :msg, r.updatedAt = :now
        where r.id = :id""")
    void markFailed(@Param("id") UUID id, @Param("msg") String msg, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("update RepoIndex r set r.status = :to, r.errorMessage = :msg, r.updatedAt = :now where r.status = :from")
    int failStuck(@Param("from") IndexStatus from, @Param("to") IndexStatus to,
                  @Param("msg") String msg, @Param("now") Instant now);

    /** Returns 0 if the repo is being indexed right now, so a delete can never race an index job. */
    @Modifying
    @Transactional
    @Query("delete from RepoIndex r where r.id = :id and r.status <> com.codereaper.Entity.IndexStatus.INDEXING")
    int deleteIfNotIndexing(@Param("id") UUID id);
}