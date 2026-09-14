package com.xagent.pilot.starter.content.application;

import com.xagent.pilot.starter.ai.domain.LanguageModel;
import com.xagent.pilot.starter.ai.domain.LlmRequest;
import com.xagent.pilot.starter.content.domain.CandidateScore;
import com.xagent.pilot.starter.content.domain.CandidateStatus;
import com.xagent.pilot.starter.content.domain.PostCandidate;
import com.xagent.pilot.starter.content.persistence.ContentCandidateEntity;
import com.xagent.pilot.starter.content.persistence.ContentCandidateRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ContentGenerationService {

    private static final int MAX_REWRITE_ATTEMPTS = 2;

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

        List<PostCandidate> candidates =
                new ArrayList<>();

        for (int variant = 1;
             variant <= count;
             variant++) {

            String content =
                    generateInitial(
                            topic,
                            tone,
                            variant
                    );

            ContentValidator.ValidationResult validation =
                    validator.validate(content);

            int attempts = 0;

            while (!validation.valid()
                    && attempts < MAX_REWRITE_ATTEMPTS) {

                content = rewrite(
                        topic,
                        tone,
                        content,
                        validation.reasons()
                );

                validation =
                        validator.validate(content);

                attempts++;
            }

            CandidateStatus status;
            CandidateScore score;

            if (!validation.valid()) {

                status = CandidateStatus.INVALID;

                score = new CandidateScore(
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
                                content
                        );

                status =
                        CandidateStatus.SCORED;
            }

            Instant now = Instant.now();

            PostCandidate candidate =
                    new PostCandidate(
                            UUID.randomUUID(),
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

    private String generateInitial(
            String topic,
            String tone,
            int variant
    ) {

        String prompt = """
                Write ONE post for X.

                Topic:
                %s

                Tone:
                %s

                Variant:
                %d

                Requirements:

                - Aim for 120 to 220 characters.
                - Hard maximum is 280 characters.
                - Return only the post.
                - No explanation.
                - No preamble.
                - No markdown wrapper.
                - Maximum 2 hashtags.
                - Maximum 1 emoji unless absolutely necessary.
                - Avoid generic AI marketing language.
                - Avoid unsupported technical claims.
                - Sound conversational and opinionated.
                - Make this variant meaningfully different.

                Write the post.
                """.formatted(
                topic,
                tone,
                variant
        );

        return languageModel.generate(
                new LlmRequest(
                        prompt,
                        0.9,
                        300,
                        "none"
                )
        ).content();
    }

    private String rewrite(
            String topic,
            String tone,
            String original,
            List<String> problems
    ) {

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

                Requirements:

                - Preserve the main idea.
                - Hard maximum 280 characters.
                - Prefer 120 to 220 characters.
                - Return only the rewritten post.
                - Maximum 2 hashtags.
                - Avoid unsupported factual claims.
                - No explanation.

                Rewrite now.
                """.formatted(
                topic,
                tone,
                original,
                String.join("; ", problems)
        );

        return languageModel.generate(
                new LlmRequest(
                        prompt,
                        0.4,
                        300,
                        "none"
                )
        ).content();
    }
}