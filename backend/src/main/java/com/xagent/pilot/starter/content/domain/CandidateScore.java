package com.xagent.pilot.starter.content.domain;

import java.util.List;

public record CandidateScore(

        int technicalAccuracy,
        int clarity,
        int naturalness,
        int engagement,
        int overall,
        List<String> issues

) {
}