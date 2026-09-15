package com.xagent.pilot.starter.research.persistence;

import com.xagent.pilot.starter.research.domain.SourceDocument;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "research_source")
public class ResearchSourceEntity {

    @Id
    private UUID id;

    @Column(
            name = "requested_url",
            nullable = false,
            columnDefinition = "text"
    )
    private String requestedUrl;

    @Column(
            name = "final_url",
            nullable = false,
            columnDefinition = "text"
    )
    private String finalUrl;

    @Column(
            nullable = false,
            columnDefinition = "text"
    )
    private String title;

    @Column(
            name = "extracted_text",
            nullable = false,
            columnDefinition = "text"
    )
    private String extractedText;

    @Column(
            name = "content_hash",
            nullable = false,
            length = 64
    )
    private String contentHash;

    @Column(
            name = "content_type",
            nullable = false
    )
    private String contentType;

    @Column(
            name = "fetched_at",
            nullable = false
    )
    private Instant fetchedAt;

    protected ResearchSourceEntity() {
    }

    public ResearchSourceEntity(SourceDocument source) {
        this.id = source.id();
        this.requestedUrl = source.requestedUrl();
        this.finalUrl = source.finalUrl();
        this.title = source.title();
        this.extractedText = source.content();
        this.contentHash = source.contentHash();
        this.contentType = source.contentType();
        this.fetchedAt = source.fetchedAt();
    }

    public SourceDocument toDomain() {

        return new SourceDocument(
                id,
                requestedUrl,
                finalUrl,
                title,
                extractedText,
                contentHash,
                contentType,
                fetchedAt
        );
    }
}