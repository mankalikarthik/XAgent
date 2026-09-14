package com.xagent.pilot.starter.ai.domain;

public record LlmRequest(
        String prompt,
        double temperature,
        int maxTokens,
        String reasoningEffort
) {

    public LlmRequest(String prompt) {
        this(
                prompt,
                0.8,
                300,
                "none"
        );
    }
}