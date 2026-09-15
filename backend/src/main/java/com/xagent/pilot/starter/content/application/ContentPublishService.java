package com.xagent.pilot.starter.content.application;

import com.xagent.pilot.starter.content.domain.CandidateStatus;
import com.xagent.pilot.starter.content.domain.PostCandidate;
import com.xagent.pilot.starter.content.persistence.ContentCandidateEntity;
import com.xagent.pilot.starter.content.persistence.ContentCandidateRepository;
import com.xagent.pilot.starter.content.port.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ContentPublishService {

    private final ContentCandidateRepository repository;
    private final Publisher publisher;

    public ContentPublishService(
            ContentCandidateRepository repository,
            Publisher publisher
    ) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public PublishResponse publish(
            UUID candidateId
    ) {

        ContentCandidateEntity entity =
                repository.findById(candidateId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Content candidate not found: "
                                                + candidateId
                                )
                        );

        PostCandidate candidate =
                entity.toDomain();

        if (candidate.status()
                != CandidateStatus.APPROVED) {

            throw new IllegalStateException(
                    "Only APPROVED candidates can be published. "
                            + "Current status: "
                            + candidate.status()
            );
        }

        Publisher.PublishResult result =
                publisher.publish(candidate);

        if (!result.success()) {

            throw new IllegalStateException(
                    "Publishing failed: "
                            + result.message()
            );
        }

        entity.markPublished(
                result.externalId(),
                result.externalUrl(),
                publisherName()
        );

        ContentCandidateEntity saved =
                repository.save(entity);

        return new PublishResponse(
                saved.toDomain(),
                saved.getExternalId(),
                saved.getExternalUrl(),
                saved.getPublishedAt(),
                saved.getPublisher(),
                result.message()
        );
    }

    private String publisherName() {

        String name =
                publisher.getClass()
                        .getSimpleName();

        if (name.endsWith("Publisher")) {

            name =
                    name.substring(
                            0,
                            name.length()
                                    - "Publisher".length()
                    );
        }

        return name.toLowerCase();
    }

    public record PublishResponse(
            PostCandidate candidate,
            String externalId,
            String externalUrl,
            Instant publishedAt,
            String publisher,
            String message
    ) {

        /*
         * Backward-compatible constructor.
         *
         * Some controller/error handling code still creates
         * PublishResponse using the older four-argument format.
         */
        public PublishResponse(
                PostCandidate candidate,
                String externalId,
                String externalUrl,
                String message
        ) {
            this(
                    candidate,
                    externalId,
                    externalUrl,
                    null,
                    null,
                    message
            );
        }
    }
}