package com.xagent.pilot.starter.content.domain;

import java.time.Instant;
import java.util.UUID;

public record PostCandidate(

        UUID id,
        UUID researchBriefId,

        String topic,
        String tone,
        String content,

        CandidateStatus status,
        CandidateScore score,

        String externalId,
        String externalUrl,
        Instant publishedAt,
        String publisher,

        Instant createdAt,
        Instant updatedAt

) {

    /*
     * Grounded or ungrounded candidate before publication.
     */
    public PostCandidate(
            UUID id,
            UUID researchBriefId,
            String topic,
            String tone,
            String content,
            CandidateStatus status,
            CandidateScore score,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id,
                researchBriefId,
                topic,
                tone,
                content,
                status,
                score,
                null,
                null,
                null,
                null,
                createdAt,
                updatedAt
        );
    }

    /*
     * Legacy / ungrounded candidate without research provenance.
     */
    public PostCandidate(
            UUID id,
            String topic,
            String tone,
            String content,
            CandidateStatus status,
            CandidateScore score,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id,
                null,
                topic,
                tone,
                content,
                status,
                score,
                null,
                null,
                null,
                null,
                createdAt,
                updatedAt
        );
    }

    /*
     * Published candidate without research provenance.
     * Kept for backward compatibility with older code paths.
     */
    public PostCandidate(
            UUID id,
            String topic,
            String tone,
            String content,
            CandidateStatus status,
            CandidateScore score,
            String externalId,
            String externalUrl,
            Instant publishedAt,
            String publisher,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id,
                null,
                topic,
                tone,
                content,
                status,
                score,
                externalId,
                externalUrl,
                publishedAt,
                publisher,
                createdAt,
                updatedAt
        );
    }
}