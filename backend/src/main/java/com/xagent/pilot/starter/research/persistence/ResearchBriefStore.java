package com.xagent.pilot.starter.research.persistence;

import com.xagent.pilot.starter.research.domain.ResearchBrief;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ResearchBriefStore {

    private final JpaResearchBriefRepository repository;
    private final JsonMapper jsonMapper;

    public ResearchBriefStore(
            JpaResearchBriefRepository repository,
            JsonMapper jsonMapper
    ) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    public Optional<ResearchBrief> find(
            UUID sourceId,
            String topic
    ) {

        return repository
                .findFirstBySourceIdAndTopic(
                        sourceId,
                        topic
                )
                .map(this::toDomain);
    }

    public Optional<ResearchBrief> findById(
            UUID id
    ) {

        return repository
                .findById(id)
                .map(this::toDomain);
    }

    public ResearchBrief save(
            ResearchBrief brief
    ) {

        try {

            String keyFactsJson =
                    jsonMapper.writeValueAsString(
                            brief.keyFacts()
                    );

            UUID id =
                    brief.id() != null
                            ? brief.id()
                            : UUID.randomUUID();

            ResearchBriefEntity entity =
                    new ResearchBriefEntity(
                            id,
                            brief.sourceId(),
                            brief.topic(),
                            brief.summary(),
                            keyFactsJson,
                            Instant.now()
                    );

            ResearchBriefEntity saved =
                    repository.save(entity);

            return toDomain(saved);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to persist research brief",
                    exception
            );
        }
    }

    private ResearchBrief toDomain(
            ResearchBriefEntity entity
    ) {

        try {

            JsonNode root =
                    jsonMapper.readTree(
                            entity.getKeyFactsJson()
                    );

            List<String> keyFacts =
                    new ArrayList<>();

            if (root.isArray()) {

                root.forEach(
                        node ->
                                keyFacts.add(
                                        node.asText()
                                )
                );
            }

            return new ResearchBrief(
                    entity.getId(),
                    entity.getSourceId(),
                    entity.getTopic(),
                    entity.getSummary(),
                    keyFacts
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to read persisted research brief",
                    exception
            );
        }
    }
}