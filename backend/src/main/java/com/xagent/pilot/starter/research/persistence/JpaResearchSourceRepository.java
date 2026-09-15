package com.xagent.pilot.starter.research.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaResearchSourceRepository
        extends JpaRepository<ResearchSourceEntity, UUID> {

    Optional<ResearchSourceEntity>
    findFirstByRequestedUrlOrderByFetchedAtDesc(
            String requestedUrl
    );

    Optional<ResearchSourceEntity>
    findFirstByFinalUrlOrderByFetchedAtDesc(
            String finalUrl
    );

    Optional<ResearchSourceEntity>
    findFirstByContentHashOrderByFetchedAtDesc(
            String contentHash
    );

    List<ResearchSourceEntity>
    findTop50ByOrderByFetchedAtDesc();

}