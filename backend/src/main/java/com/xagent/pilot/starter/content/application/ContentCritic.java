package com.xagent.pilot.starter.content.application;

import com.xagent.pilot.starter.ai.domain.LanguageModel;
import com.xagent.pilot.starter.ai.domain.LlmRequest;
import com.xagent.pilot.starter.content.domain.CandidateScore;
import com.xagent.pilot.starter.research.domain.ResearchBrief;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContentCritic {

    private final LanguageModel languageModel;
    private final JsonMapper jsonMapper;
    private final FactualClaimExtractor factualClaimExtractor;

    public ContentCritic(
            LanguageModel languageModel,
            JsonMapper jsonMapper,
            FactualClaimExtractor factualClaimExtractor
    ) {
        this.languageModel = languageModel;
        this.jsonMapper = jsonMapper;
        this.factualClaimExtractor = factualClaimExtractor;
    }

    public CandidateScore score(
            String topic,
            String content
    ) {
        return scoreUngrounded(
                topic,
                content
        );
    }

    public CandidateScore score(
            String topic,
            String content,
            ResearchBrief brief
    ) {

        if (brief == null) {
            return scoreUngrounded(
                    topic,
                    content
            );
        }

        return scoreGrounded(
                topic,
                content,
                brief
        );
    }

    private CandidateScore scoreGrounded(
            String topic,
            String content,
            ResearchBrief brief
    ) {

        List<String> claims =
                factualClaimExtractor.extract(content);

        String evidence =
                buildEvidence(brief);

        String claimsText =
                buildClaims(claims);

        String prompt = """
                You are an evidence-grounding verifier.

                Topic:
                %s

                ORIGINAL POST:
                %s

                FACTUAL CLAIMS TO VERIFY:
                %s

                SUPPLIED EVIDENCE:
                %s

                IMPORTANT:

                The factual claims have already been extracted for you.

                Verify ONLY the claims listed under
                FACTUAL CLAIMS TO VERIFY.

                DO NOT extract new claims from the original post.

                The original post is supplied only so you can score:
                - clarity
                - naturalness
                - engagement

                Use ONLY the supplied evidence when verifying claims.

                SUPPORTED:
                The evidence supports the same factual meaning.

                UNSUPPORTED:
                The claim adds factual information that the evidence
                does not establish.

                CONTRADICTED:
                The evidence explicitly states the opposite.

                RULES:

                - Missing evidence means UNSUPPORTED, not CONTRADICTED.
                - CONTRADICTED requires a direct logical conflict.
                - Paraphrasing is allowed.
                - Informal wording is allowed.
                - Do not demand more precision than the evidence contains.
                - Read ALL evidence facts before deciding.
                - Do not use outside knowledge.
                - Do not correct the supplied evidence.

                SPECIFICITY:

                If evidence says "large number":

                "large number"
                -> SUPPORTED

                "hundreds"
                -> UNSUPPORTED unless explicitly present

                "thousands"
                -> UNSUPPORTED unless explicitly present

                "millions"
                -> UNSUPPORTED unless explicitly present

                Numbers, versions, dates, percentages, benchmarks,
                guarantees and specific quantities require explicit evidence.

                EXAMPLE:

                Evidence:
                F1: Virtual threads are always daemon threads.

                Claim:
                Virtual threads are always daemon threads.

                Result:
                SUPPORTED.

                Never mark a claim contradicted when the evidence
                directly supports the same claim.

                Return ONLY actual problematic claims.

                If all claims are supported:

                "unsupportedClaims": []
                "contradictedClaims": []

                Score these separately from 0 to 100:

                clarity
                naturalness
                engagement

                Return ONLY valid JSON.
                No markdown.
                No prose outside JSON.

                {
                  "unsupportedClaims": [],
                  "contradictedClaims": [],
                  "clarity": 0,
                  "naturalness": 0,
                  "engagement": 0
                }
                """.formatted(
                topic,
                content,
                claimsText,
                evidence
        );

        for (int attempt = 0;
             attempt < 2;
             attempt++) {

            String raw =
                    languageModel.generate(
                            new LlmRequest(
                                    prompt,
                                    0.0,
                                    500,
                                    "none"
                            )
                    ).content();

            try {

                JsonNode root =
                        jsonMapper.readTree(
                                extractJson(raw)
                        );

                List<String> issues =
                        new ArrayList<>();

                int unsupported =
                        collectIssues(
                                root.path(
                                        "unsupportedClaims"
                                ),
                                "UNSUPPORTED",
                                issues
                        );

                int contradicted =
                        collectIssues(
                                root.path(
                                        "contradictedClaims"
                                ),
                                "CONTRADICTED",
                                issues
                        );

                int technicalAccuracy;

                if (contradicted > 0) {

                    technicalAccuracy = 40;

                } else if (unsupported > 0) {

                    technicalAccuracy = 70;

                } else {

                    technicalAccuracy = 100;
                }

                int clarity =
                        clamp(
                                root.path("clarity")
                                        .asInt(50)
                        );

                int naturalness =
                        clamp(
                                root.path("naturalness")
                                        .asInt(50)
                        );

                int engagement =
                        clamp(
                                root.path("engagement")
                                        .asInt(50)
                        );

                return new CandidateScore(
                        technicalAccuracy,
                        clarity,
                        naturalness,
                        engagement,
                        calculateOverall(
                                technicalAccuracy,
                                clarity,
                                naturalness,
                                engagement
                        ),
                        issues
                );

            } catch (Exception ignored) {

                // Retry once if local model produces malformed JSON.
            }
        }

        return parseFailure();
    }

    private String buildClaims(
            List<String> claims
    ) {

        if (claims.isEmpty()) {
            return "NO FACTUAL CLAIMS";
        }

        StringBuilder output =
                new StringBuilder();

        for (int i = 0;
             i < claims.size();
             i++) {

            output.append("C")
                    .append(i + 1)
                    .append(": ")
                    .append(
                            claims.get(i)
                    )
                    .append("\n");
        }

        return output.toString();
    }

    private int collectIssues(
            JsonNode node,
            String prefix,
            List<String> issues
    ) {

        if (!node.isArray()) {
            return 0;
        }

        int count = 0;

        for (JsonNode item : node) {

            String claim =
                    item.asText("")
                            .trim();

            if (!claim.isBlank()) {

                issues.add(
                        prefix
                                + ": "
                                + claim
                );

                count++;
            }
        }

        return count;
    }

    private CandidateScore scoreUngrounded(
            String topic,
            String content
    ) {

        String prompt = """
                Review this X post.

                Topic:
                %s

                Post:
                %s

                Score each category from 0 to 100:

                - technicalAccuracy
                - clarity
                - naturalness
                - engagement

                Return ONLY valid JSON:

                {
                  "technicalAccuracy": 0,
                  "clarity": 0,
                  "naturalness": 0,
                  "engagement": 0,
                  "issues": []
                }
                """.formatted(
                topic,
                content
        );

        String raw =
                languageModel.generate(
                        new LlmRequest(
                                prompt,
                                0.1,
                                500,
                                "none"
                        )
                ).content();

        try {

            JsonNode root =
                    jsonMapper.readTree(
                            extractJson(raw)
                    );

            int technical =
                    clamp(
                            root.path(
                                    "technicalAccuracy"
                            ).asInt(50)
                    );

            int clarity =
                    clamp(
                            root.path(
                                    "clarity"
                            ).asInt(50)
                    );

            int naturalness =
                    clamp(
                            root.path(
                                    "naturalness"
                            ).asInt(50)
                    );

            int engagement =
                    clamp(
                            root.path(
                                    "engagement"
                            ).asInt(50)
                    );

            List<String> issues =
                    new ArrayList<>();

            JsonNode issueNode =
                    root.path("issues");

            if (issueNode.isArray()) {

                issueNode.forEach(
                        node ->
                                issues.add(
                                        node.asText()
                                )
                );
            }

            return new CandidateScore(
                    technical,
                    clarity,
                    naturalness,
                    engagement,
                    calculateOverall(
                            technical,
                            clarity,
                            naturalness,
                            engagement
                    ),
                    issues
            );

        } catch (Exception exception) {

            return parseFailure();
        }
    }

    private String buildEvidence(
            ResearchBrief brief
    ) {

        StringBuilder evidence =
                new StringBuilder();

        for (int i = 0;
             i < brief.keyFacts().size();
             i++) {

            evidence.append("F")
                    .append(i + 1)
                    .append(": ")
                    .append(
                            brief.keyFacts().get(i)
                    )
                    .append("\n");
        }

        return evidence.toString();
    }

    private int calculateOverall(
            int technical,
            int clarity,
            int naturalness,
            int engagement
    ) {

        return (int) Math.round(
                technical * 0.35
                        + clarity * 0.20
                        + naturalness * 0.25
                        + engagement * 0.20
        );
    }

    private CandidateScore parseFailure() {

        return new CandidateScore(
                0,
                0,
                0,
                0,
                0,
                List.of(
                        "Critic response could not be parsed after retry"
                )
        );
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

    private int clamp(
            int value
    ) {

        return Math.max(
                0,
                Math.min(
                        100,
                        value
                )
        );
    }
}