package com.xagent.pilot.starter.content.infrastructure;

import com.xagent.pilot.starter.content.domain.PostCandidate;
import com.xagent.pilot.starter.content.port.Publisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("local-publisher")
public class LocalPublisher implements Publisher {

    @Override
    public PublishResult publish(
            PostCandidate candidate
    ) {

        String externalId =
                "local-" + UUID.randomUUID();

        return new PublishResult(
                true,
                externalId,
                null,
                "Published using local development publisher"
        );
    }
}