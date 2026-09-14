package com.xagent.pilot.starter.content.application;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.xagent.pilot.starter.ai.domain.LanguageModel;
import com.xagent.pilot.starter.ai.domain.LlmRequest;
import com.xagent.pilot.starter.content.domain.CandidateScore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContentCritic {

    private final LanguageModel languageModel;
    private final JsonMapper objectMapper;

    public ContentCritic(
            LanguageModel languageModel,
            JsonMapper objectMapper
    ) {
        this.languageModel = languageModel;
        this.objectMapper = objectMapper;
    }

    public CandidateScore score(
            String topic,
            String content
    ) {

        String prompt = """
                You are reviewing a proposed X post.

                Topic:
                %s

                Post:
                %s

                Evaluate it conservatively.

                Score each category from 0 to 100:

                technicalAccuracy:
                Does it avoid obvious factual or technical errors?

                clarity:
                Is the idea understandable and concise?

                naturalness:
                Does it sound like something a real person might write,
                rather than generic AI marketing copy?

                engagement:
                Is it interesting enough that someone might reply,
                repost, bookmark, or continue reading?

                Return ONLY valid JSON using this exact structure:

                {
                  "technicalAccuracy": 0,
                  "clarity": 0,
                  "naturalness": 0,
                  "engagement": 0,
                  "issues": [
                    "issue 1",
                    "issue 2"
                  ]
                }

                Do not include markdown.
                """.formatted(
                topic,
                content
        );

        String raw = languageModel.generate(
                new LlmRequest(
                        prompt,
                        0.1,
                        500,
                        "none"
                )
        ).content();

        try {

            String json = extractJson(raw);

            JsonNode root =
                    objectMapper.readTree(json);

            int technical =
                    clamp(root.path(
                            "technicalAccuracy"
                    ).asInt(50));

            int clarity =
                    clamp(root.path(
                            "clarity"
                    ).asInt(50));

            int naturalness =
                    clamp(root.path(
                            "naturalness"
                    ).asInt(50));

            int engagement =
                    clamp(root.path(
                            "engagement"
                    ).asInt(50));

            List<String> issues =
                    new ArrayList<>();

            JsonNode issueNode =
                    root.path("issues");

            if (issueNode.isArray()) {

                issueNode.forEach(
                        node -> issues.add(
                                node.asText()
                        )
                );
            }

            int overall = (int) Math.round(
                    technical * 0.35
                            + clarity * 0.20
                            + naturalness * 0.25
                            + engagement * 0.20
            );

            return new CandidateScore(
                    technical,
                    clarity,
                    naturalness,
                    engagement,
                    overall,
                    issues
            );

        } catch (Exception exception) {

            return new CandidateScore(
                    50,
                    50,
                    50,
                    50,
                    50,
                    List.of(
                            "Critic response could not be parsed"
                    )
            );
        }
    }

    private String extractJson(String raw) {

        String text = raw.trim();

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return text.substring(
                    start,
                    end + 1
            );
        }

        return text;
    }

    private int clamp(int value) {
        return Math.max(
                0,
                Math.min(100, value)
        );
    }
}