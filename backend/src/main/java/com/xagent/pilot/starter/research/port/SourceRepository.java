package com.xagent.pilot.starter.research.port;

import com.xagent.pilot.starter.research.domain.SourceDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceRepository {

    SourceDocument save(SourceDocument source);

    Optional<SourceDocument> findById(UUID id);

    Optional<SourceDocument> findByRequestedUrl(String requestedUrl);

    Optional<SourceDocument> findByFinalUrl(String finalUrl);

    Optional<SourceDocument> findByContentHash(String contentHash);

    List<SourceDocument> recent();

}