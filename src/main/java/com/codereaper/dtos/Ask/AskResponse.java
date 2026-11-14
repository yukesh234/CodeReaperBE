package com.codereaper.dtos.Ask;

import java.util.List;

public record AskResponse(String answer, List<Source> sources) {
    public record Source(String path, Integer startLine) {}
}