package com.ratelimiter.service;

import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JokeService {

    private final Faker faker;

    public String pun() {
        return faker.joke().pun();
    }

    public String getKnockKnockJoke() {
        return faker.joke().knockKnock();
    }
}
