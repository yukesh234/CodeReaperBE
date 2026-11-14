package com.codereaper.Service.indexing;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class GitHubApiClient {

    public record TreeEntry(String path, String sha, long size) {}
    public record Tree(List<TreeEntry> entries, boolean truncated) {}

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};
    private final RestClient client;

    public GitHubApiClient(RestClient.Builder builder) {   // Boot's builder
        this.client = builder
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public String headSha(String token, String fullName) {
        return (String) get(token, "/repos/" + fullName + "/commits/HEAD").get("sha");
    }

    @SuppressWarnings("unchecked")
    public Tree tree(String token, String fullName, String commitSha) {
        Map<String, Object> m = get(token, "/repos/" + fullName + "/git/trees/" + commitSha + "?recursive=1");
        List<Map<String, Object>> raw = (List<Map<String, Object>>) m.get("tree");
        List<TreeEntry> entries = raw.stream()
                .filter(e -> "blob".equals(e.get("type")))
                .map(e -> new TreeEntry(
                        (String) e.get("path"),
                        (String) e.get("sha"),
                        e.get("size") == null ? 0 : ((Number) e.get("size")).longValue()))
                .toList();
        return new Tree(entries, Boolean.TRUE.equals(m.get("truncated")));
    }

    /** File text, or null if it looks binary. */
    public String blob(String token, String fullName, String blobSha) {
        Map<String, Object> m = get(token, "/repos/" + fullName + "/git/blobs/" + blobSha);
        String content = (String) m.get("content");
        if (content == null || !"base64".equals(m.get("encoding"))) return null;
        byte[] bytes = Base64.getMimeDecoder().decode(content);   // GitHub wraps lines
        for (byte b : bytes) if (b == 0) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Map<String, Object> get(String token, String uri) {
        try {
            return client.get().uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(MAP);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new IllegalStateException("GitHub token is no longer valid");
        }
    }
    private Map<String, Object> getRepoInfo(String token, String uri) {
        try {
            return get(token, uri);
        } catch (HttpClientErrorException e) {
            int code = e.getStatusCode().value();
            throw new IllegalStateException(switch (code) {
                case 404 -> "GitHub can't find this repository, or the token has no access to it";
                case 409 -> "This repository is empty";
                case 403, 429 -> "GitHub rate limit reached, try again later";
                default -> "GitHub request failed (" + code + ")";
            });
        }
    }
}