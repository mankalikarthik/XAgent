package com.xagent.pilot.starter.content.application;

import com.xagent.pilot.starter.content.domain.CandidateStatus;
import com.xagent.pilot.starter.content.domain.PostCandidate;
import com.xagent.pilot.starter.content.persistence.ContentCandidateEntity;
import com.xagent.pilot.starter.content.persistence.ContentCandidateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ContentCandidateService {

    private final ContentCandidateRepository repository;

    public ContentCandidateService(
            ContentCandidateRepository repository
    ) {
        this.repository = repository;
    }

    public List<PostCandidate> recent() {

        return repository
                .findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(ContentCandidateEntity::toDomain)
                .toList();
    }

    public PostCandidate approve(UUID id) {

        ContentCandidateEntity entity =
                get(id);

        if (entity.getStatus()
                != CandidateStatus.SCORED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only SCORED candidates can be approved"
            );
        }

        entity.updateStatus(
                CandidateStatus.APPROVED
        );

        repository.save(entity);

        return entity.toDomain();
    }

    public PostCandidate reject(UUID id) {

        ContentCandidateEntity entity =
                get(id);

        if (entity.getStatus()
                == CandidateStatus.PUBLISHED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Published content cannot be rejected"
            );
        }

        entity.updateStatus(
                CandidateStatus.REJECTED
        );

        repository.save(entity);

        return entity.toDomain();
    }

    private ContentCandidateEntity get(UUID id) {

        return repository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Candidate not found"
                        )
                );
    }
}