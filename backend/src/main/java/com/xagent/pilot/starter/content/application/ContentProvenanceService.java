package com.xagent.pilot.starter.content.application;



import com.xagent.pilot.starter.content.domain.PostCandidate;
import com.xagent.pilot.starter.content.persistence.ContentCandidateRepository;
import com.xagent.pilot.starter.research.domain.ResearchBrief;
import com.xagent.pilot.starter.research.domain.SourceDocument;
import com.xagent.pilot.starter.research.persistence.ResearchBriefStore;
import com.xagent.pilot.starter.research.port.SourceRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ContentProvenanceService {

    private final ContentCandidateRepository candidateRepository;
    private final ResearchBriefStore researchBriefStore;
    private final SourceRepository sourceRepository;

    public ContentProvenanceService(
            ContentCandidateRepository candidateRepository,
            ResearchBriefStore researchBriefStore,
            SourceRepository sourceRepository
    ) {
        this.candidateRepository = candidateRepository;
        this.researchBriefStore = researchBriefStore;
        this.sourceRepository = sourceRepository;
    }

    public ContentProvenance getProvenance(
            UUID candidateId
    ) {

        PostCandidate candidate =
                candidateRepository
                        .findById(candidateId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Content candidate not found: "
                                                + candidateId
                                )
                        )
                        .toDomain();

        if (candidate.researchBriefId() == null) {

            return new ContentProvenance(
                    candidate,
                    null,
                    null
            );
        }

        ResearchBrief brief =
                researchBriefStore
                        .findById(
                                candidate.researchBriefId()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Research brief not found: "
                                                + candidate.researchBriefId()
                                )
                        );

        SourceDocument source =
                sourceRepository
                        .findById(
                                brief.sourceId()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Research source not found: "
                                                + brief.sourceId()
                                )
                        );

        return new ContentProvenance(
                candidate,
                brief,
                source
        );
    }

    public record ContentProvenance(
            PostCandidate candidate,
            ResearchBrief researchBrief,
            SourceDocument source
    ) {
    }
}