package com.xagent.pilot.starter.content.persistence;

import com.xagent.pilot.starter.content.domain.CandidateScore;
import com.xagent.pilot.starter.content.domain.CandidateStatus;
import com.xagent.pilot.starter.content.domain.PostCandidate;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "content_candidate")
public class ContentCandidateEntity {

    @Id
    private UUID id;

    @Column(name = "research_brief_id")
    private UUID researchBriefId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String tone;

    @Column(
            nullable = false,
            columnDefinition = "text"
    )
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidateStatus status;

    private int technicalAccuracy;

    private int clarity;

    private int naturalness;

    private int engagement;

    private int overallScore;

    @Column(columnDefinition = "text")
    private String issues;

    @Column(name = "external_id")
    private String externalId;

    @Column(
            name = "external_url",
            columnDefinition = "text"
    )
    private String externalUrl;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publisher")
    private String publisher;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ContentCandidateEntity() {
    }

    public ContentCandidateEntity(
            PostCandidate candidate
    ) {

        CandidateScore score =
                candidate.score();

        this.id =
                candidate.id();

        this.researchBriefId =
                candidate.researchBriefId();

        this.topic =
                candidate.topic();

        this.tone =
                candidate.tone();

        this.content =
                candidate.content();

        this.status =
                candidate.status();

        this.technicalAccuracy =
                score.technicalAccuracy();

        this.clarity =
                score.clarity();

        this.naturalness =
                score.naturalness();

        this.engagement =
                score.engagement();

        this.overallScore =
                score.overall();

        this.issues =
                String.join(
                        System.lineSeparator(),
                        score.issues()
                );

        this.externalId =
                candidate.externalId();

        this.externalUrl =
                candidate.externalUrl();

        this.publishedAt =
                candidate.publishedAt();

        this.publisher =
                candidate.publisher();

        this.createdAt =
                candidate.createdAt();

        this.updatedAt =
                candidate.updatedAt();
    }

    public void updateStatus(
            CandidateStatus status
    ) {

        this.status =
                status;

        this.updatedAt =
                Instant.now();
    }

    public void markPublished(
            String externalId,
            String externalUrl,
            String publisher
    ) {

        Instant now =
                Instant.now();

        this.status =
                CandidateStatus.PUBLISHED;

        this.externalId =
                externalId;

        this.externalUrl =
                externalUrl;

        this.publisher =
                publisher;

        this.publishedAt =
                now;

        this.updatedAt =
                now;
    }

    public CandidateStatus getStatus() {
        return status;
    }

    public UUID getResearchBriefId() {
        return researchBriefId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getPublisher() {
        return publisher;
    }

    public PostCandidate toDomain() {

        List<String> issueList =
                issues == null
                        || issues.isBlank()
                        ? List.of()
                        : Arrays.asList(
                        issues.split("\\R")
                );

        CandidateScore score =
                new CandidateScore(
                        technicalAccuracy,
                        clarity,
                        naturalness,
                        engagement,
                        overallScore,
                        issueList
                );

        return new PostCandidate(
                id,
                researchBriefId,
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