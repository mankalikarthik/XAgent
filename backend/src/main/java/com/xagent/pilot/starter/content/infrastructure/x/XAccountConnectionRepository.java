package com.xagent.pilot.starter.content.infrastructure.x;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface XAccountConnectionRepository
        extends JpaRepository<XAccountConnectionEntity, UUID> {

    @Query("""
            SELECT connection
            FROM XAccountConnectionEntity connection
            WHERE connection.xUserId = :xUserId
            """)
    Optional<XAccountConnectionEntity> findByXUserId(
            @Param("xUserId") String xUserId
    );

    Optional<XAccountConnectionEntity>
    findTopByOrderByUpdatedAtDesc();
}