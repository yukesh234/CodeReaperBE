package com.codereaper.Service.indexing;

import com.codereaper.Entity.IndexStatus;
import com.codereaper.Repo.RepoIndexRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class IndexRecovery {

    private final RepoIndexRepo repo;

    @EventListener(ApplicationReadyEvent.class)
    public void failInterrupted() {
        repo.failStuck(IndexStatus.INDEXING, IndexStatus.FAILED,
                "Interrupted by a server restart", Instant.now());
    }
}