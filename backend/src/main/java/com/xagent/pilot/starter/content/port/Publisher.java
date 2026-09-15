package com.xagent.pilot.starter.content.port;

import com.xagent.pilot.starter.content.domain.PostCandidate;

public interface Publisher {

    PublishResult publish(
            PostCandidate candidate
    );

    record PublishResult(
            boolean success,
            String externalId,
            String externalUrl,
            String message
    ) {
    }
}