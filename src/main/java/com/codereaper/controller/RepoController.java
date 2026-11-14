package com.codereaper.controller;

import com.codereaper.Service.RepoService;
import com.codereaper.Service.ai.AskService;
import com.codereaper.dtos.Ask.AskRequest;
import com.codereaper.dtos.Ask.AskResponse;
import com.codereaper.dtos.Ask.MessageDto;
import com.codereaper.dtos.Repoindex.RepoDto;
import com.codereaper.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoController {

    private final CurrentUser currentUser;
    private final RepoService repoService;
    private final AskService askService;

    @GetMapping
    public List<RepoDto> list() {
        return repoService.list(currentUser.get());
    }

    @PostMapping("/{id}/index")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void index(@PathVariable long id) {
        repoService.startIndex(currentUser.get(), id);
    }
    @GetMapping("/{id}/status")
    public RepoService.RepoStatusDto status(@PathVariable long id) {
        return repoService.status(currentUser.get(), id);
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<Map<String, String>> sync(@PathVariable long id) {
        boolean started = repoService.sync(currentUser.get(), id);
        return started
                ? ResponseEntity.accepted().body(Map.of("status", "indexing"))
                : ResponseEntity.ok(Map.of("status", "up_to_date"));
    }

    @PostMapping(value = "/{id}/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> askStream(@PathVariable long id, @RequestBody AskRequest request) {
        return askService.askStream(currentUser.get(), id, request == null ? null : request.question());
    }

    @GetMapping("/{id}/messages")
    public List<MessageDto> messages(@PathVariable long id) {
        return askService.messages(currentUser.get(), id);
    }

    @DeleteMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearMessages(@PathVariable long id) {
        askService.clearMessages(currentUser.get(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        repoService.delete(currentUser.get(), id);
    }
}