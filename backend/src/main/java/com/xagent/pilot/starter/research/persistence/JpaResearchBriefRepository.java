package com.xagent.pilot.starter.research.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaResearchBriefRepository
        extends JpaRepository<ResearchBriefEntity, UUID> {

    Optional<ResearchBriefEntity> findFirstBySourceIdAndTopic(
            UUID sourceId,
            String topic
    );
}