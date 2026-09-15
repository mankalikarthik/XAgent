package com.xagent.pilot.starter.research.domain;

import java.net.URI;
import java.time.Instant;

public record FetchedPage(

        URI requestedUri,
        URI finalUri,
        int statusCode,
        String contentType,
        byte[] body,
        Instant fetchedAt

) {
}