package com.xagent.pilot.starter.content.api;

import com.xagent.pilot.starter.content.domain.PostCandidate;

import java.util.List;

public record GenerateCandidatesResponse(

        List<PostCandidate> candidates

) {
}