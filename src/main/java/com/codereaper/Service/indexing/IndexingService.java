package com.codereaper.Service.indexing;

import com.codereaper.Entity.RepoIndex;
import com.codereaper.Entity.User;
import com.codereaper.Repo.RepoIndexRepo;
import com.codereaper.Service.UserService;
import com.codereaper.Service.ai.RagSettings;
import com.codereaper.Service.indexing.GitHubApiClient.TreeEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

    private static final int VECTOR_BATCH_SIZE = 32;
    private static final int PROGRESS_EVERY_N_FILES = 5;
    private static final long MAX_FILE_BYTES = 102_400;
    private static final int MAX_FILES = 500;
    private final RepoIndexRepo repoIndexRepo;
    private final UserService userService;
    private final GitHubApiClient github;
    private final CodeFileFilter fileFilter;
    private final CodeChunker chunker;
    private final VectorStore vectorStore;

    /** Call this from a different bean (RepoService), or @Async won't apply. */
    @Async("indexingExecutor")
    public void indexAsync(UUID repoIndexId) {
        try {
            doIndex(repoIndexId);
        } catch (Exception e) {
            log.error("Indexing failed for repo_index {}", repoIndexId, e);
            try {
                repoIndexRepo.markFailed(repoIndexId, truncate(e.getMessage()), Instant.now());
            } catch (Exception inner) {
                log.error("Could not record failure for {}", repoIndexId, inner);
            }
        }
    }

    private void doIndex(UUID id) {
        RepoIndex repo = repoIndexRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("repo_index not found: " + id));
        User user = userService.requireById(repo.getUserId());
        String token = user.getAccessToken();   // the converter already decrypted it
        String fullName = repo.getFullName();

        // Pin the whole run to one commit
        String sha = github.headSha(token, fullName);
        GitHubApiClient.Tree tree = github.tree(token, fullName, sha);
        if (tree.truncated()) {
            log.warn("GitHub truncated the file tree for {}; indexing a partial listing", fullName);
        }


        List<TreeEntry> files = tree.entries().stream()
                .filter(e -> fileFilter.isEligible(e.path(), e.size(), MAX_FILE_BYTES))
                .sorted(java.util.Comparator
                        .comparingInt((TreeEntry e) -> e.path().split("/").length)
                        .thenComparing(TreeEntry::path))
                .limit(MAX_FILES)
                .toList();
        if (files.isEmpty()) {
            throw new IllegalStateException("No supported source files found in this repository");
        }
        repoIndexRepo.updateTotal(id, files.size(), Instant.now());

        deleteExistingVectors(id);

        Map<String, Object> baseMeta = Map.of(
                RagSettings.META_REPO_ID, id.toString(),
                RagSettings.META_USER_ID, repo.getUserId().toString(),
                "commit", sha);

        List<Document> batch = new ArrayList<>(VECTOR_BATCH_SIZE);
        int processed = 0;
        int chunks = 0;

        for (TreeEntry file : files) {
            String content = fetch(token, fullName, file);
            if (content != null) {
                for (Document d : chunker.chunkFile(file.path(), content, baseMeta)) {
                    batch.add(d);
                    chunks++;
                    if (batch.size() >= VECTOR_BATCH_SIZE) flush(batch);
                }
            }
            processed++;
            if (processed % PROGRESS_EVERY_N_FILES == 0) {
                repoIndexRepo.updateProgress(id, processed, chunks, Instant.now());
            }
        }
        flush(batch);

        repoIndexRepo.markReady(id, sha, processed, chunks, Instant.now());
        log.info("Indexed {} ({} files, {} chunks)", fullName, processed, chunks);
    }

    /** Null means skip this file (binary, or a single-file GitHub error). */
    private String fetch(String token, String fullName, TreeEntry file) {
        try {
            return github.blob(token, fullName, file.sha());
        } catch (HttpClientErrorException e) {
            int code = e.getStatusCode().value();
            if (code == 403 || code == 429) throw e;   // rate limited: abort the whole run
            log.warn("Skipping {} ({})", file.path(), code);
            return null;
        } catch (RestClientException e) {
            log.warn("Skipping {} ({})", file.path(), e.getMessage());
            return null;
        }
    }

    private void flush(List<Document> batch) {
        if (batch.isEmpty()) return;
        vectorStore.add(List.copyOf(batch));   // embeds, then stores
        batch.clear();
    }

    private void deleteExistingVectors(UUID repoIndexId) {
        var b = new FilterExpressionBuilder();
        vectorStore.delete(b.eq(RagSettings.META_REPO_ID, repoIndexId.toString()).build());
    }

    private static String truncate(String s) {
        if (s == null) return "Unknown error";
        return s.length() > 900 ? s.substring(0, 900) : s;
    }
}