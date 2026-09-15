package com.xagent.pilot.starter.research.application;

import com.xagent.pilot.starter.research.domain.FetchedPage;
import com.xagent.pilot.starter.research.domain.SourceDocument;
import com.xagent.pilot.starter.research.infrastructure.web.HtmlSourceExtractor;
import com.xagent.pilot.starter.research.infrastructure.web.UrlSafetyValidator;
import com.xagent.pilot.starter.research.port.InternetTool;
import com.xagent.pilot.starter.research.port.SourceRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
public class ResearchSourceService {

    private final InternetTool internetTool;
    private final SourceRepository sourceRepository;
    private final HtmlSourceExtractor extractor;
    private final UrlSafetyValidator urlSafetyValidator;

    public ResearchSourceService(
            InternetTool internetTool,
            SourceRepository sourceRepository,
            HtmlSourceExtractor extractor,
            UrlSafetyValidator urlSafetyValidator
    ) {
        this.internetTool = internetTool;
        this.sourceRepository = sourceRepository;
        this.extractor = extractor;
        this.urlSafetyValidator = urlSafetyValidator;
    }

    public SourceDocument ingest(String rawUrl) {

        URI requested =
                urlSafetyValidator.validate(rawUrl);

        return sourceRepository
                .findByRequestedUrl(requested.toString())
                .orElseGet(() -> fetchAndStore(requested));
    }

    private SourceDocument fetchAndStore(URI requested) {

        FetchedPage page =
                internetTool.fetch(requested);

        var existingFinal =
                sourceRepository.findByFinalUrl(
                        page.finalUri().toString()
                );

        if (existingFinal.isPresent()) {
            return existingFinal.get();
        }

        SourceDocument extracted =
                extractor.extract(page);

        var existingHash =
                sourceRepository.findByContentHash(
                        extracted.contentHash()
                );

        if (existingHash.isPresent()) {
            return existingHash.get();
        }

        return sourceRepository.save(extracted);
    }

    public List<SourceDocument> recent() {
        return sourceRepository.recent();
    }
}