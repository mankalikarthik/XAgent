package com.xagent.pilot.starter.content.infrastructure.x;

import com.xagent.pilot.starter.content.domain.PostCandidate;
import com.xagent.pilot.starter.content.port.Publisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class XPublisher implements Publisher {

    private static final String CREATE_POST_URL =
            "https://api.x.com/2/tweets";

    private final XOAuthService oauthService;
    private final JsonMapper jsonMapper;

    private final RestClient restClient =
            RestClient.create();

    public XPublisher(
            XOAuthService oauthService,
            JsonMapper jsonMapper
    ) {
        this.oauthService = oauthService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public PublishResult publish(
            PostCandidate candidate
    ) {

        try {

            String accessToken =
                    oauthService
                            .getValidAccessToken();

            Map<String, Object> payload =
                    new LinkedHashMap<>();

            payload.put(
                    "text",
                    candidate.content()
            );

            payload.put(
                    "made_with_ai",
                    true
            );

            String raw =
                    restClient.post()
                            .uri(
                                    CREATE_POST_URL
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .headers(
                                    headers ->
                                            headers.setBearerAuth(
                                                    accessToken
                                            )
                            )
                            .body(payload)
                            .retrieve()
                            .body(String.class);

            JsonNode data =
                    jsonMapper.readTree(raw)
                            .path("data");

            String postId =
                    data.path("id")
                            .asText();

            if (postId == null
                    || postId.isBlank()) {

                return new PublishResult(
                        false,
                        null,
                        null,
                        "X API did not return a post ID: "
                                + raw
                );
            }

            String externalUrl =
                    "https://x.com/i/web/status/"
                            + postId;

            return new PublishResult(
                    true,
                    postId,
                    externalUrl,
                    "Published successfully to X"
            );

        } catch (RestClientResponseException exception) {

            return new PublishResult(
                    false,
                    null,
                    null,
                    "X API returned HTTP "
                            + exception.getStatusCode()
                            + ": "
                            + exception.getResponseBodyAsString()
            );

        } catch (Exception exception) {

            return new PublishResult(
                    false,
                    null,
                    null,
                    "X publishing failed: "
                            + exception.getMessage()
            );
        }
    }
}