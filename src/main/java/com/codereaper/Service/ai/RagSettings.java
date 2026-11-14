package com.codereaper.Service.ai;

public final class RagSettings {
    public static final int TOP_K_CHUNKS = 8; // how many code chunks to fetch from the vector database per question
    public static final long STREAM_TIMEOUT_MS = 180_000L; // max time to keep a sse stream open while the model is responding
//    public static final String META_REPO_ID = "repoid";
    public static final String META_REPO_ID = "repoIndexId";
    public static final String META_USER_ID = "userId";

    private RagSettings() {}



}
