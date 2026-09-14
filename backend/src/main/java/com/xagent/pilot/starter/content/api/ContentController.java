package com.xagent.pilot.starter.content.api;

import com.xagent.pilot.starter.content.application.ContentCandidateService;
import com.xagent.pilot.starter.content.application.ContentGenerationService;
import com.xagent.pilot.starter.content.domain.PostCandidate;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    private final ContentGenerationService generationService;
    private final ContentCandidateService candidateService;

    public ContentController(
            ContentGenerationService generationService,
            ContentCandidateService candidateService
    ) {
        this.generationService = generationService;
        this.candidateService = candidateService;
    }

    @PostMapping("/candidates")
    public GenerateCandidatesResponse generateCandidates(
            @Valid
            @RequestBody
            GenerateCandidatesRequest request
    ) {

        return new GenerateCandidatesResponse(
                generationService.generate(
                        request.topic(),
                        request.tone(),
                        request.count()
                )
        );
    }

    @GetMapping("/candidates")
    public List<PostCandidate> recentCandidates() {
        return candidateService.recent();
    }

    @PatchMapping(
            "/candidates/{id}/approve"
    )
    public PostCandidate approve(
            @PathVariable UUID id
    ) {
        return candidateService.approve(id);
    }

    @PatchMapping(
            "/candidates/{id}/reject"
    )
    public PostCandidate reject(
            @PathVariable UUID id
    ) {
        return candidateService.reject(id);
    }
}