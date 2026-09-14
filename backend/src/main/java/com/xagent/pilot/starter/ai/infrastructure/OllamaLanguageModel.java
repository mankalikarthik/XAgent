package com.xagent.pilot.starter.ai.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xagent.pilot.starter.ai.domain.LanguageModel;
import com.xagent.pilot.starter.ai.domain.LlmRequest;
import com.xagent.pilot.starter.ai.domain.LlmResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OllamaLanguageModel implements LanguageModel {

    private final RestClient restClient;
    private final String model;

    public OllamaLanguageModel(
            @Value("${xagent.ai.base-url}") String baseUrl,
            @Value("${xagent.ai.model}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.model = model;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {

        if (request == null ||
                request.prompt() == null ||
                request.prompt().isBlank()) {

            throw new IllegalArgumentException("Prompt cannot be blank");
        }

        OllamaChatRequest ollamaRequest =
                new OllamaChatRequest(
                        model,
                        List.of(
                                new ChatMessage(
                                        "user",
                                        request.prompt()
                                )
                        ),
                        false,
                        request.temperature(),
                        request.maxTokens(),
                        request.reasoningEffort()
                );

        OllamaChatResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(ollamaRequest)
                .retrieve()
                .body(OllamaChatResponse.class);

        if (response == null ||
                response.choices() == null ||
                response.choices().isEmpty() ||
                response.choices().getFirst().message() == null) {

            throw new IllegalStateException(
                    "LLM returned an empty response"
            );
        }

        String content =
                response.choices()
                        .getFirst()
                        .message()
                        .content();

        if (content == null || content.isBlank()) {
            throw new IllegalStateException(
                    "LLM returned no final content"
            );
        }

        return new LlmResponse(content.trim());
    }

    private record OllamaChatRequest(
            String model,
            List<ChatMessage> messages,
            boolean stream,
            double temperature,

            @JsonProperty("max_tokens")
            int maxTokens,

            @JsonProperty("reasoning_effort")
            String reasoningEffort
    ) {
    }

    private record ChatMessage(
            String role,
            String content
    ) {
    }

    private record OllamaChatResponse(
            List<Choice> choices
    ) {
    }

    private record Choice(
            ResponseMessage message
    ) {
    }

    private record ResponseMessage(
            String role,
            String content
    ) {
    }
}