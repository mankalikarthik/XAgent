package com.xagent.pilot.starter.research.api;

import com.xagent.pilot.starter.research.application.ResearchBriefService;
import com.xagent.pilot.starter.research.application.ResearchSourceService;
import com.xagent.pilot.starter.research.domain.ResearchBrief;
import com.xagent.pilot.starter.research.domain.SourceDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/research")
public class ResearchController {

    private final ResearchSourceService researchSourceService;
    private final ResearchBriefService researchBriefService;

    public ResearchController(
            ResearchSourceService researchSourceService,
            ResearchBriefService researchBriefService
    ) {
        this.researchSourceService = researchSourceService;
        this.researchBriefService = researchBriefService;
    }

    @PostMapping("/sources")
    public SourceDocument ingest(
            @Valid @RequestBody IngestSourceRequest request
    ) {
        return researchSourceService.ingest(
                request.url()
        );
    }

    @GetMapping("/sources")
    public List<SourceDocument> recent() {
        return researchSourceService.recent();
    }

    @PostMapping("/brief")
    public ResearchBrief createBrief(
            @Valid @RequestBody ResearchBriefRequest request
    ) {
        return researchBriefService.createBrief(
                request.sourceId(),
                request.topic()
        );
    }

    public record IngestSourceRequest(
            @NotBlank
            String url
    ) {
    }

    public record ResearchBriefRequest(
            @NotNull
            UUID sourceId,

            @NotBlank
            String topic
    ) {
    }
}