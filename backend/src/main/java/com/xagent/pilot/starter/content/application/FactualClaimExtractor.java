package com.xagent.pilot.starter.content.application;

import com.xagent.pilot.starter.ai.domain.LanguageModel;
import com.xagent.pilot.starter.ai.domain.LlmRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class FactualClaimExtractor {

    private final LanguageModel languageModel;
    private final JsonMapper jsonMapper;

    public FactualClaimExtractor(
            LanguageModel languageModel,
            JsonMapper jsonMapper
    ) {
        this.languageModel = languageModel;
        this.jsonMapper = jsonMapper;
    }

    public List<String> extract(String content) {

        String prompt = """
                Extract ONLY factual claims from this X post.

                Post:
                %s

                A factual claim is something that could be true or false.

                Do NOT extract:

                - opinions
                - jokes
                - sarcasm
                - metaphors
                - rhetorical language
                - recommendations
                - commands
                - stylistic phrases

                Examples:

                "Virtual threads feel like magic."
                -> no factual claim

                "Stop pooling virtual threads."
                -> no factual claim

                "Virtual threads use M:N scheduling."
                -> factual claim

                "Virtual threads are always daemon threads."
                -> factual claim

                "Virtual threads let you run millions of tasks."
                -> factual claim

                Split compound statements into separate atomic claims.

                Return ONLY valid JSON:

                {
                  "claims": [
                    "claim 1",
                    "claim 2"
                  ]
                }
                """.formatted(content);

        String raw =
                languageModel.generate(
                        new LlmRequest(
                                prompt,
                                0.0,
                                350,
                                "none"
                        )
                ).content();

        try {

            JsonNode root =
                    jsonMapper.readTree(
                            extractJson(raw)
                    );

            List<String> claims =
                    new ArrayList<>();

            JsonNode nodes =
                    root.path("claims");

            if (nodes.isArray()) {

                nodes.forEach(node -> {

                    String claim =
                            node.asText("")
                                    .trim();

                    if (!claim.isBlank()) {
                        claims.add(claim);
                    }
                });
            }

            return claims;

        } catch (Exception exception) {

            return List.of();
        }
    }

    private String extractJson(String raw) {

        String text =
                raw.trim();

        int start =
                text.indexOf('{');

        int end =
                text.lastIndexOf('}');

        if (start >= 0 && end > start) {

            return text.substring(
                    start,
                    end + 1
            );
        }

        return text;
    }
}