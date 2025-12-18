package com.ratelimiter.controller;

import com.ratelimiter.dto.JokeResponse;
import com.ratelimiter.service.JokeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class JokeController {

    private final JokeService jokeService;

    @GetMapping("/jokes/pun")
    public ResponseEntity<JokeResponse> pun() {
        return ResponseEntity.ok(new JokeResponse("pun", jokeService.pun()));
    }

    @GetMapping("/jokes/knock-knock")
    public ResponseEntity<JokeResponse> knockKnock() {
        return ResponseEntity.ok(new JokeResponse("knock-knock", jokeService.getKnockKnockJoke()));
    }

}
