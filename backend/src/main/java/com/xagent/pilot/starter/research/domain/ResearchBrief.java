package com.xagent.pilot.starter.research.domain;

import java.util.List;
import java.util.UUID;

public record ResearchBrief(

        UUID id,
        UUID sourceId,
        String topic,
        String summary,
        List<String> keyFacts

) {

    public ResearchBrief(
            UUID sourceId,
            String topic,
            String summary,
            List<String> keyFacts
    ) {
        this(
                null,
                sourceId,
                topic,
                summary,
                keyFacts
        );
    }
}