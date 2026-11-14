package com.codereaper.dtos.Ask;

import java.util.List;

public record MessageDto(String id, String role, String content, List<AskResponse.Source> sources) {}