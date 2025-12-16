package com.ratelimiter.model;

public enum Tiers {

    FREE("free"),
    PREMIUM("premium"),
    ENTERPRISE("enterprise");

    private final String value;

    Tiers(String value) {
        this.value = value;
    }

}
