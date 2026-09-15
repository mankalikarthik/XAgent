package com.xagent.pilot.starter.research.domain;

import java.time.Instant;
import java.util.UUID;

public record SourceDocument(

        UUID id,
        String requestedUrl,
        String finalUrl,
        String title,
        String content,
        String contentHash,
        String contentType,
        Instant fetchedAt

) {
}