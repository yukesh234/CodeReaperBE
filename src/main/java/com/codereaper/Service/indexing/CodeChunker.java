package com.codereaper.Service.indexing;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CodeChunker {

    private static final int CHUNK_TOKENS = 150;

    private final TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(CHUNK_TOKENS).build();
    private final CodeFileFilter fileFilter;

    public CodeChunker(CodeFileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    public List<Document> chunkFile(String path, String content, Map<String, Object> baseMeta) {
        if (content == null || content.isBlank()) return List.of();

        String language = fileFilter.detectLanguage(path);
        Document source = Document.builder().text(content).metadata(new HashMap<>(baseMeta)).build();
        List<Document> pieces = splitter.apply(List.of(source));

        List<Document> out = new ArrayList<>(pieces.size());
        int cursor = 0;     // where to search for the next chunk
        int linePos = 0;    // offset that `line` refers to
        int line = 1;

        for (int i = 0; i < pieces.size(); i++) {
            String body = pieces.get(i).getText();
            Map<String, Object> meta = new HashMap<>(pieces.get(i).getMetadata());
            meta.put("filePath", path);
            meta.put("language", language);
            meta.put("chunkIndex", i);

            String header = "File: " + path;
            int at = content.indexOf(body, cursor);
            if (at >= 0) {
                for (int p = linePos; p < at; p++) if (content.charAt(p) == '\n') line++;
                linePos = at;
                int end = line;
                for (int p = at; p < at + body.length(); p++) if (content.charAt(p) == '\n') end++;
                meta.put("startLine", line);
                meta.put("endLine", end);
                header += " (lines " + line + "-" + end + ")";
                cursor = at + 1;
            }
            out.add(Document.builder().text(header + "\n" + body).metadata(meta).build());
        }
        return out;
    }
}