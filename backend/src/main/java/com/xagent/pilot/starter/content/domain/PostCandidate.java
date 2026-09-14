package com.xagent.pilot.starter.content.domain;

import java.time.Instant;
import java.util.UUID;

public record PostCandidate(

        UUID id,
        String topic,
        String tone,
        String content,
        CandidateStatus status,
        CandidateScore score,
        Instant createdAt,
        Instant updatedAt

) {
}