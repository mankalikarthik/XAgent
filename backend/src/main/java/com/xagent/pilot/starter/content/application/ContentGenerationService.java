package com.xagent.pilot.starter.content.application;

import com.xagent.pilot.starter.ai.domain.LanguageModel;
import com.xagent.pilot.starter.ai.domain.LlmRequest;
import com.xagent.pilot.starter.content.domain.CandidateScore;
import com.xagent.pilot.starter.content.domain.CandidateStatus;
import com.xagent.pilot.starter.content.domain.PostCandidate;
import com.xagent.pilot.starter.content.persistence.ContentCandidateEntity;
import com.xagent.pilot.starter.content.persistence.ContentCandidateRepository;
import com.xagent.pilot.starter.research.domain.ResearchBrief;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ContentGenerationService {

    private static final int MAX_REWRITE_ATTEMPTS = 3;
    private static final int MAX_GROUNDING_REPAIR_ATTEMPTS = 2;

    private final LanguageModel languageModel;
    private final ContentValidator validator;
    private final ContentCritic critic;
    private final ContentCandidateRepository repository;

    public ContentGenerationService(
            LanguageModel languageModel,
            ContentValidator validator,
            ContentCritic critic,
            ContentCandidateRepository repository
    ) {
        this.languageModel = languageModel;
        this.validator = validator;
        this.critic = critic;
        this.repository = repository;
    }

    public List<PostCandidate> generate(
            String topic,
            String tone,
            int count
    ) {
        return generateInternal(
                topic,
                tone,
                count,
                null
        );
    }

    public List<PostCandidate> generateGrounded(
            String topic,
            String tone,
            int count,
            ResearchBrief brief
    ) {
        return generateInternal(
                topic,
                tone,
                count,
                brief
        );
    }

    private List<PostCandidate> generateInternal(
            String topic,
            String tone,
            int count,
            ResearchBrief brief
    ) {

        List<PostCandidate> candidates =
                new ArrayList<>();

        for (int variant = 1;
             variant <= count;
             variant++) {

            String content =
                    generateInitial(
                            topic,
                            tone,
                            variant,
                            brief
                    );

            ContentValidator.ValidationResult validation =
                    validator.validate(content);

            int rewriteAttempts = 0;

            while (!validation.valid()
                    && rewriteAttempts < MAX_REWRITE_ATTEMPTS) {

                content =
                        rewrite(
                                topic,
                                tone,
                                content,
                                validation.reasons(),
                                brief
                        );

                validation =
                        validator.validate(content);

                rewriteAttempts++;
            }

            CandidateStatus status;
            CandidateScore score;

            if (!validation.valid()) {

                status = CandidateStatus.INVALID;

                score =
                        new CandidateScore(
                                0,
                                0,
                                0,
                                0,
                                0,
                                validation.reasons()
                        );

            } else {

                score =
                        critic.score(
                                topic,
                                content,
                                brief
                        );

                if (brief != null) {

                    int groundingAttempts = 0;

                    while (needsGroundingRepair(score)
                            && groundingAttempts
                            < MAX_GROUNDING_REPAIR_ATTEMPTS) {

                        content =
                                repairGrounding(
                                        topic,
                                        tone,
                                        content,
                                        score.issues(),
                                        brief
                                );

                        validation =
                                validator.validate(content);

                        if (!validation.valid()) {

                            content =
                                    rewrite(
                                            topic,
                                            tone,
                                            content,
                                            validation.reasons(),
                                            brief
                                    );

                            validation =
                                    validator.validate(content);
                        }

                        if (!validation.valid()) {
                            break;
                        }

                        score =
                                critic.score(
                                        topic,
                                        content,
                                        brief
                                );

                        groundingAttempts++;
                    }
                }

                if (!validation.valid()) {

                    status = CandidateStatus.INVALID;

                    score =
                            new CandidateScore(
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    validation.reasons()
                            );

                } else {

                    status = CandidateStatus.SCORED;
                }
            }

            Instant now =
                    Instant.now();

            PostCandidate candidate =
                    new PostCandidate(
                            UUID.randomUUID(),
                            brief == null
                                    ? null
                                    : brief.id(),
                            topic,
                            tone,
                            content,
                            status,
                            score,
                            now,
                            now
                    );

            repository.save(
                    new ContentCandidateEntity(
                            candidate
                    )
            );

            candidates.add(candidate);
        }

        return candidates;
    }

    private boolean needsGroundingRepair(
            CandidateScore score
    ) {

        return score.technicalAccuracy() == 70
                || score.technicalAccuracy() == 40;
    }

    private String generateInitial(
            String topic,
            String tone,
            int variant,
            ResearchBrief brief
    ) {

        String researchContext =
                brief == null
                        ? "No external research context provided."
                        : buildResearchContext(brief);

        String prompt = """
                Write ONE post for X.

                Topic:
                %s

                Tone:
                %s

                Variant:
                %d

                Research evidence:
                %s

                Requirements:

                - TARGET LENGTH: 130 to 180 characters.
                - ABSOLUTE MAXIMUM: 220 characters.
                - Use ONE main factual idea only.
                - Maximum 2 sentences.
                - No hashtags.
                - No emojis.
                - Return only the post.
                - No explanation.
                - No preamble.
                - No markdown wrapper.
                - Avoid generic AI marketing language.
                - Sound conversational and opinionated.
                - Make this variant meaningfully different.

                IMPORTANT:

                - If research evidence is provided, factual claims MUST
                  be supported by it.
                - Do not invent technical details.
                - Do not add factual claims absent from the evidence.
                - Do not invent numbers, quantities, versions, dates,
                  benchmarks, percentages, limits, or performance claims.
                - If the evidence says "large number", do NOT rewrite it
                  as "hundreds", "thousands", or "millions".
                - Preserve the level of certainty and specificity
                  in the evidence.
                - You may express an opinion based on the evidence.

                Write the post.
                """.formatted(
                topic,
                tone,
                variant,
                researchContext
        );

        return languageModel.generate(
                new LlmRequest(
                        prompt,
                        0.7,
                        120,
                        "none"
                )
        ).content();
    }

    private String rewrite(
            String topic,
            String tone,
            String original,
            List<String> problems,
            ResearchBrief brief
    ) {

        String researchContext =
                brief == null
                        ? "No external research context provided."
                        : buildResearchContext(brief);

        String prompt = """
                Rewrite this X post.

                Topic:
                %s

                Tone:
                %s

                Original:
                %s

                Problems:
                %s

                Research evidence:
                %s

                Requirements:

                - Produce ONE short post.
                - TARGET LENGTH: 140 to 190 characters.
                - ABSOLUTE MAXIMUM: 220 characters.
                - Keep only ONE main factual idea.
                - Remove secondary details instead of compressing everything.
                - Maximum 2 sentences.
                - No hashtags.
                - No emojis.
                - No explanation.
                - Return only the rewritten post.
                - Any factual claim must be supported by the research evidence.
                - Do not invent numbers, quantities, versions, dates,
                  benchmarks, percentages, or performance claims.
                - Preserve the specificity of the supplied evidence.

                Rewrite now.
                """.formatted(
                topic,
                tone,
                original,
                String.join("; ", problems),
                researchContext
        );

        return languageModel.generate(
                new LlmRequest(
                        prompt,
                        0.2,
                        140,
                        "none"
                )
        ).content();
    }

    private String repairGrounding(
            String topic,
            String tone,
            String original,
            List<String> issues,
            ResearchBrief brief
    ) {

        String prompt = """
                Repair this X post so every factual claim is grounded
                in the supplied research evidence.

                Topic:
                %s

                Tone:
                %s

                Original post:
                %s

                Grounding problems:
                %s

                Research evidence:
                %s

                Rules:

                - Fix every UNSUPPORTED or CONTRADICTED claim.
                - Do not add new factual claims.
                - Prefer deleting an unsupported detail rather than replacing
                  it with another guessed detail.
                - Preserve the main idea.
                - Preserve the personality, joke, sarcasm, or opinion
                  when it does not create a factual claim.
                - Preserve the evidence's level of specificity.
                - If evidence says "large number", keep "large number"
                  or use another equally non-specific phrase.
                - Never convert it to hundreds, thousands, millions,
                  or another number unless explicitly supported.
                - TARGET LENGTH: 130 to 180 characters.
                - ABSOLUTE MAXIMUM: 220 characters.
                - Maximum 2 sentences.
                - No hashtags.
                - No emojis.
                - Return ONLY the rewritten post.

                Rewrite now.
                """.formatted(
                topic,
                tone,
                original,
                String.join(
                        System.lineSeparator(),
                        issues
                ),
                buildResearchContext(brief)
        );

        return languageModel.generate(
                new LlmRequest(
                        prompt,
                        0.2,
                        140,
                        "none"
                )
        ).content();
    }

    private String buildResearchContext(
            ResearchBrief brief
    ) {

        String facts =
                String.join(
                        System.lineSeparator() + "- ",
                        brief.keyFacts()
                );

        return """
                Summary:
                %s

                Key facts:
                - %s
                """.formatted(
                brief.summary(),
                facts
        );
    }
}