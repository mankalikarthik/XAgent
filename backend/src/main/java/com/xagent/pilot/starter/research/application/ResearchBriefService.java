package com.xagent.pilot.starter.research.application;

import com.xagent.pilot.starter.ai.domain.LanguageModel;
import com.xagent.pilot.starter.ai.domain.LlmRequest;
import com.xagent.pilot.starter.research.domain.ResearchBrief;
import com.xagent.pilot.starter.research.domain.SourceDocument;
import com.xagent.pilot.starter.research.persistence.ResearchBriefStore;
import com.xagent.pilot.starter.research.port.SourceRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResearchBriefService {

    private static final int MAX_CHUNKS = 5;

    private final SourceRepository sourceRepository;
    private final ResearchBriefStore researchBriefStore;
    private final ResearchTextChunker chunker;
    private final LanguageModel languageModel;
    private final JsonMapper jsonMapper;

    public ResearchBriefService(
            SourceRepository sourceRepository,
            ResearchBriefStore researchBriefStore,
            ResearchTextChunker chunker,
            LanguageModel languageModel,
            JsonMapper jsonMapper
    ) {
        this.sourceRepository = sourceRepository;
        this.researchBriefStore = researchBriefStore;
        this.chunker = chunker;
        this.languageModel = languageModel;
        this.jsonMapper = jsonMapper;
    }

    public ResearchBrief createBrief(
            UUID sourceId,
            String topic
    ) {

        String normalizedTopic =
                topic.trim();

        Optional<ResearchBrief> existing =
                researchBriefStore.find(
                        sourceId,
                        normalizedTopic
                );

        if (existing.isPresent()) {
            return existing.get();
        }

        SourceDocument source =
                sourceRepository.findById(sourceId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Research source not found: "
                                                + sourceId
                                )
                        );

        ResearchBrief generated =
                generateBrief(
                        source,
                        normalizedTopic
                );

        return researchBriefStore.save(
                generated
        );
    }

    private ResearchBrief generateBrief(
            SourceDocument source,
            String topic
    ) {

        List<String> chunks =
                chunker.chunk(
                        source.content()
                );

        List<String> selectedChunks =
                selectRelevantChunks(
                        chunks,
                        topic
                );

        List<String> extractedFacts =
                new ArrayList<>();

        for (String chunk : selectedChunks) {

            String prompt = """
                    You are extracting factual information from a source.

                    Topic:
                    %s

                    Source excerpt:
                    %s

                    Extract only facts from this excerpt that are relevant
                    to the topic.

                    Rules:

                    - Use only information present in the excerpt.
                    - Do not add outside knowledge.
                    - Do not speculate.
                    - Return at most 5 concise bullet points.
                    - If nothing is relevant, return NONE.
                    """.formatted(
                    topic,
                    chunk
            );

            String result =
                    languageModel.generate(
                            new LlmRequest(
                                    prompt,
                                    0.1,
                                    400,
                                    "none"
                            )
                    ).content();

            if (!result.trim()
                    .equalsIgnoreCase("NONE")) {

                extractedFacts.add(result);
            }
        }

        return consolidate(
                source.id(),
                topic,
                extractedFacts
        );
    }

    private ResearchBrief consolidate(
            UUID sourceId,
            String topic,
            List<String> extractedFacts
    ) {

        String evidence =
                String.join(
                        "\n\n",
                        extractedFacts
                );

        String prompt = """
                Create a research brief using ONLY the evidence below.

                Topic:
                %s

                Evidence:
                %s

                Return ONLY valid JSON:

                {
                  "summary": "short factual summary",
                  "keyFacts": [
                    "fact 1",
                    "fact 2"
                  ]
                }

                Rules:

                - Do not add information not present in the evidence.
                - Remove duplicate facts.
                - Maximum 8 key facts.
                - Keep the summary concise.
                """.formatted(
                topic,
                evidence
        );

        String raw =
                languageModel.generate(
                        new LlmRequest(
                                prompt,
                                0.1,
                                700,
                                "none"
                        )
                ).content();

        try {

            String json =
                    extractJson(raw);

            JsonNode root =
                    jsonMapper.readTree(json);

            String summary =
                    root.path("summary")
                            .asText();

            List<String> keyFacts =
                    new ArrayList<>();

            JsonNode facts =
                    root.path("keyFacts");

            if (facts.isArray()) {

                facts.forEach(
                        node ->
                                keyFacts.add(
                                        node.asText()
                                )
                );
            }

            return new ResearchBrief(
                    sourceId,
                    topic,
                    summary,
                    keyFacts
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to parse research brief",
                    exception
            );
        }
    }

    private List<String> selectRelevantChunks(
            List<String> chunks,
            String topic
    ) {

        Set<String> keywords =
                Arrays.stream(
                                topic.toLowerCase()
                                        .split("\\W+")
                        )
                        .filter(
                                word ->
                                        word.length() >= 3
                        )
                        .collect(
                                Collectors.toSet()
                        );

        return chunks.stream()
                .sorted(
                        Comparator
                                .comparingInt(
                                        (String chunk) ->
                                                relevanceScore(
                                                        chunk,
                                                        keywords
                                                )
                                )
                                .reversed()
                )
                .limit(MAX_CHUNKS)
                .toList();
    }

    private int relevanceScore(
            String chunk,
            Set<String> keywords
    ) {

        String lower =
                chunk.toLowerCase();

        int score = 0;

        for (String keyword : keywords) {

            int index = 0;

            while ((index =
                    lower.indexOf(
                            keyword,
                            index
                    )) >= 0) {

                score++;
                index += keyword.length();
            }
        }

        return score;
    }

    private String extractJson(
            String raw
    ) {

        String text =
                raw.trim();

        int start =
                text.indexOf('{');

        int end =
                text.lastIndexOf('}');

        if (start >= 0
                && end > start) {

            return text.substring(
                    start,
                    end + 1
            );
        }

        return text;
    }
}