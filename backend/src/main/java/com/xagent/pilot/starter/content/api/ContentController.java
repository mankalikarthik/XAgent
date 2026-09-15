package com.xagent.pilot.starter.content.api;

import com.xagent.pilot.starter.content.application.ContentCandidateService;
import com.xagent.pilot.starter.content.application.ContentGenerationService;
import com.xagent.pilot.starter.content.application.ContentProvenanceService;
import com.xagent.pilot.starter.content.application.ContentPublishService;
import com.xagent.pilot.starter.content.domain.PostCandidate;
import com.xagent.pilot.starter.research.application.ResearchBriefService;
import com.xagent.pilot.starter.research.domain.ResearchBrief;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    private final ContentGenerationService generationService;
    private final ContentCandidateService candidateService;
    private final ResearchBriefService researchBriefService;
    private final ContentProvenanceService provenanceService;
    private final ContentPublishService publishService;

    public ContentController(
            ContentGenerationService generationService,
            ContentCandidateService candidateService,
            ResearchBriefService researchBriefService,
            ContentProvenanceService provenanceService,
            ContentPublishService publishService
    ) {
        this.generationService = generationService;
        this.candidateService = candidateService;
        this.researchBriefService = researchBriefService;
        this.provenanceService = provenanceService;
        this.publishService = publishService;
    }

    @PostMapping("/candidates")
    public GenerateCandidatesResponse generateCandidates(
            @Valid @RequestBody GenerateCandidatesRequest request
    ) {

        return new GenerateCandidatesResponse(
                generationService.generate(
                        request.topic(),
                        request.tone(),
                        request.count()
                )
        );
    }

    @PostMapping("/grounded-candidates")
    public GenerateCandidatesResponse generateGroundedCandidates(
            @Valid @RequestBody GroundedCandidatesRequest request
    ) {

        ResearchBrief brief =
                researchBriefService.createBrief(
                        request.sourceId(),
                        request.topic()
                );

        return new GenerateCandidatesResponse(
                generationService.generateGrounded(
                        request.topic(),
                        request.tone(),
                        request.count(),
                        brief
                )
        );
    }

    @GetMapping("/candidates")
    public List<PostCandidate> recentCandidates() {

        return candidateService.recent();
    }

    @GetMapping("/candidates/{id}/provenance")
    public ContentProvenanceService.ContentProvenance provenance(
            @PathVariable UUID id
    ) {

        return provenanceService.getProvenance(id);
    }

    @PatchMapping("/candidates/{id}/approve")
    public PostCandidate approve(
            @PathVariable UUID id
    ) {

        return candidateService.approve(id);
    }

    @PatchMapping("/candidates/{id}/reject")
    public PostCandidate reject(
            @PathVariable UUID id
    ) {

        return candidateService.reject(id);
    }

    @PostMapping("/candidates/{id}/publish")
    public ContentPublishService.PublishResponse publish(
            @PathVariable UUID id
    ) {

        return publishService.publish(id);
    }

    public record GroundedCandidatesRequest(

            @NotNull
            UUID sourceId,

            @NotBlank
            String topic,

            @NotBlank
            String tone,

            @Min(1)
            @Max(5)
            int count

    ) {
    }
}