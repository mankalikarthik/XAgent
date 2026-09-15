package com.xagent.pilot.starter.research.persistence;

import com.xagent.pilot.starter.research.domain.SourceDocument;
import com.xagent.pilot.starter.research.port.SourceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresSourceRepository
        implements SourceRepository {

    private final JpaResearchSourceRepository repository;

    public PostgresSourceRepository(
            JpaResearchSourceRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public SourceDocument save(SourceDocument source) {

        return repository.save(
                new ResearchSourceEntity(source)
        ).toDomain();
    }

    @Override
    public Optional<SourceDocument> findById(UUID id) {

        return repository
                .findById(id)
                .map(ResearchSourceEntity::toDomain);
    }

    @Override
    public Optional<SourceDocument> findByRequestedUrl(
            String requestedUrl
    ) {

        return repository
                .findFirstByRequestedUrlOrderByFetchedAtDesc(
                        requestedUrl
                )
                .map(ResearchSourceEntity::toDomain);
    }

    @Override
    public Optional<SourceDocument> findByFinalUrl(
            String finalUrl
    ) {

        return repository
                .findFirstByFinalUrlOrderByFetchedAtDesc(
                        finalUrl
                )
                .map(ResearchSourceEntity::toDomain);
    }

    @Override
    public Optional<SourceDocument> findByContentHash(
            String contentHash
    ) {

        return repository
                .findFirstByContentHashOrderByFetchedAtDesc(
                        contentHash
                )
                .map(ResearchSourceEntity::toDomain);
    }

    @Override
    public List<SourceDocument> recent() {

        return repository
                .findTop50ByOrderByFetchedAtDesc()
                .stream()
                .map(ResearchSourceEntity::toDomain)
                .toList();
    }
}