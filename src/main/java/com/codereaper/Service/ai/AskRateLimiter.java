package com.codereaper.Service.ai;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AskRateLimiter {

    private static final int MAX_REQUESTS = 20;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final Map<UUID, Deque<Instant>> recent = new ConcurrentHashMap<>();

    public boolean tryAcquire(UUID userId) {
        Instant now = Instant.now();
        Deque<Instant> q = recent.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (q) {
            Instant cutoff = now.minus(WINDOW);
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            if (q.size() >= MAX_REQUESTS) return false;
            q.addLast(now);
            return true;
        }
    }
}