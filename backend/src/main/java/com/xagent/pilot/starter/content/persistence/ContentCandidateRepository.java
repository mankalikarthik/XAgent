package com.xagent.pilot.starter.content.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentCandidateRepository
        extends JpaRepository<ContentCandidateEntity, UUID> {

    List<ContentCandidateEntity>
    findTop50ByOrderByCreatedAtDesc();

}