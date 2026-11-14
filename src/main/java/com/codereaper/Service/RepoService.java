package com.codereaper.Service;

import com.codereaper.Entity.IndexStatus;
import com.codereaper.Entity.RepoIndex;
import com.codereaper.Entity.User;
import com.codereaper.Repo.ChatMessageRepo;
import com.codereaper.Repo.RepoIndexRepo;
import com.codereaper.Service.indexing.IndexingService;
import com.codereaper.dtos.Repoindex.RepoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepoService {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 5;
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {};

    private  final ChatMessageRepo chatMessageRepo;
    private final RepoIndexRepo indexRepo;
    private final IndexingService indexingService;
    private final VectorStore vectorStore;

    private final RestClient github = RestClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();

    public record RepoStatusDto(String status, int filesProcessed, int filesTotal,
                                int chunkCount, String errorMessage, String indexedCommitSha) {}

    // reads

    public List<RepoDto> list(User user) {
        List<Map<String, Object>> remote = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            List<Map<String, Object>> batch =
                    githubGet(user, "/user/repos?per_page=" + PAGE_SIZE + "&sort=updated&page=" + page, LIST);
            if (batch == null || batch.isEmpty()) break;
            remote.addAll(batch);
            if (batch.size() < PAGE_SIZE) break;
        }

        Map<Long, RepoIndex> indexes = indexRepo.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(RepoIndex::getGithubRepoId, java.util.function.Function.identity(), (a, b) -> a));

        return remote.stream().map(r -> {
            long id = ((Number) r.get("id")).longValue();
            RepoIndex ri = indexes.get(id);
            return new RepoDto(
                    id,
                    (String) r.get("full_name"),
                    (String) r.get("description"),
                    (String) r.get("language"),
                    Boolean.TRUE.equals(r.get("private")),
                    (String) r.get("default_branch"),
                    ri == null ? "none" : ri.getStatus().name().toLowerCase(),
                    ri == null ? 0 : ri.getFilesProcessed(),
                    ri == null ? 0 : ri.getFilesTotal(),
                    ri == null ? 0 : ri.getChunkCount(),
                    ri == null ? null : ri.getErrorMessage());
        }).toList();
    }

    public RepoStatusDto status(User user, long githubRepoId) {
        RepoIndex ri = find(user, githubRepoId);
        return new RepoStatusDto(ri.getStatus().name().toLowerCase(), ri.getFilesProcessed(),
                ri.getFilesTotal(), ri.getChunkCount(), ri.getErrorMessage(), ri.getIndexedCommitSha());
    }

    //  writes (not @Transactional on purpose: each save must be committed before the async job starts)

    public void startIndex(User user, long githubRepoId) {
        // Asking GitHub with the user's own token proves they can actually see this repo
        Map<String, Object> remote = githubGet(user, "/repositories/" + githubRepoId, MAP);
        String fullName = (String) remote.get("full_name");

        RepoIndex ri = indexRepo.findByUserIdAndGithubRepoId(user.getId(), githubRepoId)
                .orElseGet(() -> createRow(user, githubRepoId, fullName));

        if (!fullName.equals(ri.getFullName())) {   // repo was renamed on GitHub
            ri.setFullName(fullName);
            indexRepo.save(ri);
        }
        begin(ri.getId());
    }

    /** @return true if a re-index was started, false if the repo is already up to date */
    public boolean sync(User user, long githubRepoId) {
        RepoIndex ri = find(user, githubRepoId);
        if (ri.getStatus() == IndexStatus.INDEXING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already indexing");
        }

        Map<String, Object> commit = githubGet(user, "/repos/" + ri.getFullName() + "/commits/HEAD", MAP);
        String latest = (String) commit.get("sha");
        if (ri.getStatus() == IndexStatus.READY && latest.equals(ri.getIndexedCommitSha())) {
            return false;
        }

        begin(ri.getId());
        return true;
    }

    //  helpers

    /** Atomically flips the row to INDEXING (fails if it already is), then starts the background job. */
    private void begin(UUID repoIndexId) {
        if (indexRepo.claimForIndexing(repoIndexId, Instant.now()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already indexing");
        }
        try {
            indexingService.indexAsync(repoIndexId);
        } catch (TaskRejectedException e) {
            indexRepo.markFailed(repoIndexId, "Indexing queue is full, try again shortly", Instant.now());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Indexing queue is full");
        }
    }

    private RepoIndex createRow(User user, long githubRepoId, String fullName) {
        RepoIndex ri = new RepoIndex();
        ri.setUserId(user.getId());
        ri.setGithubRepoId(githubRepoId);
        ri.setFullName(fullName);
        ri.setUpdatedAt(Instant.now());
        try {
            return indexRepo.save(ri);
        } catch (DataIntegrityViolationException e) {   // a concurrent request created it first
            return indexRepo.findByUserIdAndGithubRepoId(user.getId(), githubRepoId)
                    .orElseThrow(() -> e);
        }
    }

    private RepoIndex find(User user, long githubRepoId) {
        return indexRepo.findByUserIdAndGithubRepoId(user.getId(), githubRepoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not indexed yet"));
    }

    private <T> T githubGet(User user, String uri, ParameterizedTypeReference<T> type) {
        try {
            return github.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + user.getAccessToken())
                    .retrieve()
                    .body(type);
        } catch (HttpClientErrorException e) {
            int code = e.getStatusCode().value();
            if (code == 401) throw new ResponseStatusException(HttpStatus.CONFLICT, "GitHub token is no longer valid");
            if (code == 404) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found or no longer accessible");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GitHub request failed (" + code + ")");
        }
    }
    public void delete(User user, long githubRepoId) {
        RepoIndex ri = find(user, githubRepoId);
        if (ri.getStatus() == IndexStatus.INDEXING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wait for indexing to finish");
        }
        var b = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();
        vectorStore.delete(b.eq(com.codereaper.Service.ai.RagSettings.META_REPO_ID, ri.getId().toString()).build());
        chatMessageRepo.deleteAllFor(ri.getId(), user.getId());
        if (indexRepo.deleteIfNotIndexing(ri.getId()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wait for indexing to finish");
        }
    }
}