package com.xagent.pilot.starter.research.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "research_brief",
        uniqueConstraints = @UniqueConstraint(
                name = "idx_research_brief_source_topic",
                columnNames = {
                        "source_id",
                        "topic"
                }
        )
)
public class ResearchBriefEntity {

    @Id
    private UUID id;

    @Column(
            name = "source_id",
            nullable = false
    )
    private UUID sourceId;

    @Column(
            nullable = false,
            columnDefinition = "text"
    )
    private String topic;

    @Column(
            nullable = false,
            columnDefinition = "text"
    )
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "key_facts",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String keyFactsJson;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    protected ResearchBriefEntity() {
    }

    public ResearchBriefEntity(
            UUID id,
            UUID sourceId,
            String topic,
            String summary,
            String keyFactsJson,
            Instant createdAt
    ) {
        this.id = id;
        this.sourceId = sourceId;
        this.topic = topic;
        this.summary = summary;
        this.keyFactsJson = keyFactsJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getTopic() {
        return topic;
    }

    public String getSummary() {
        return summary;
    }

    public String getKeyFactsJson() {
        return keyFactsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}