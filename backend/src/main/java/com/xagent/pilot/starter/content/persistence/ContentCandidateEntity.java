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

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String tone;

    @Column(nullable = false, columnDefinition = "text")
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

        this.id = candidate.id();
        this.topic = candidate.topic();
        this.tone = candidate.tone();
        this.content = candidate.content();
        this.status = candidate.status();

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

        this.createdAt =
                candidate.createdAt();

        this.updatedAt =
                candidate.updatedAt();
    }

    public void updateStatus(
            CandidateStatus status
    ) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public CandidateStatus getStatus() {
        return status;
    }

    public PostCandidate toDomain() {

        List<String> issueList =
                issues == null ||
                        issues.isBlank()
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
                topic,
                tone,
                content,
                status,
                score,
                createdAt,
                updatedAt
        );
    }
}