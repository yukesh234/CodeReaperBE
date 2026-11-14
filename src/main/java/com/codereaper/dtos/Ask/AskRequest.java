package com.codereaper.dtos.Ask;

import java.util.List;

public record AskRequest(String question, List<Turn> history) {
    public record Turn(String role, String content) {}
}