package com.xagent.pilot.starter.content.infrastructure.x;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class XOAuthService {

    private static final String USER_ME_URL =
            "https://api.x.com/2/users/me";

    private final XOAuthProperties properties;
    private final XOAuthSessionStore sessionStore;
    private final JsonMapper jsonMapper;

    private final RestClient restClient =
            RestClient.create();

    private final SecureRandom secureRandom =
            new SecureRandom();

    public XOAuthService(
            XOAuthProperties properties,
            XOAuthSessionStore sessionStore,
            JsonMapper jsonMapper
    ) {
        this.properties = properties;
        this.sessionStore = sessionStore;
        this.jsonMapper = jsonMapper;
    }

    public URI createAuthorizationUri() {

        String state =
                randomValue(32);

        String verifier =
                randomValue(64);

        String challenge =
                createChallenge(verifier);

        sessionStore.beginAuthorization(
                state,
                verifier
        );

        return UriComponentsBuilder
                .fromUriString(
                        properties.getAuthorizeUrl()
                )
                .queryParam(
                        "response_type",
                        "code"
                )
                .queryParam(
                        "client_id",
                        properties.getClientId()
                )
                .queryParam(
                        "redirect_uri",
                        properties.getRedirectUri()
                )
                .queryParam(
                        "scope",
                        String.join(
                                " ",
                                properties.getScopes()
                        )
                )
                .queryParam(
                        "state",
                        state
                )
                .queryParam(
                        "code_challenge",
                        challenge
                )
                .queryParam(
                        "code_challenge_method",
                        "S256"
                )
                .build()
                .encode()
                .toUri();
    }

    public ConnectionStatus completeAuthorization(
            String code,
            String state
    ) {

        String verifier =
                sessionStore
                        .consumeCodeVerifier(
                                state
                        );

        TokenResponse tokenResponse =
                exchangeAuthorizationCode(
                        code,
                        verifier
                );

        sessionStore.saveTokens(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn()
        );

        loadCurrentUser(
                tokenResponse.accessToken()
        );

        return status();
    }

    public synchronized String getValidAccessToken() {

        if (!sessionStore.isConnected()) {

            throw new IllegalStateException(
                    "X account is not connected. "
                            + "Open /api/v1/x/oauth/connect first."
            );
        }

        if (sessionStore.expiresSoon()) {

            refreshAccessToken();
        }

        return sessionStore.getAccessToken();
    }

    public ConnectionStatus status() {

        return new ConnectionStatus(
                sessionStore.isConnected(),
                sessionStore.getUserId(),
                sessionStore.getUsername(),
                sessionStore.getName(),
                sessionStore.getExpiresAt(),
                sessionStore.getRefreshToken()
                        != null
                        && !sessionStore
                        .getRefreshToken()
                        .isBlank()
        );
    }

    private TokenResponse exchangeAuthorizationCode(
            String code,
            String verifier
    ) {

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add(
                "grant_type",
                "authorization_code"
        );

        form.add(
                "code",
                code
        );

        form.add(
                "redirect_uri",
                properties.getRedirectUri()
        );

        form.add(
                "code_verifier",
                verifier
        );

        String raw =
                restClient.post()
                        .uri(
                                properties.getTokenUrl()
                        )
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED
                        )
                        .headers(
                                headers ->
                                        headers.setBasicAuth(
                                                properties.getClientId(),
                                                properties.getClientSecret(),
                                                StandardCharsets.UTF_8
                                        )
                        )
                        .body(form)
                        .retrieve()
                        .body(String.class);

        return parseTokenResponse(raw);
    }

    private void refreshAccessToken() {

        String refreshToken =
                sessionStore.getRefreshToken();

        if (refreshToken == null
                || refreshToken.isBlank()) {

            throw new IllegalStateException(
                    "X access token expired and no refresh token is available."
            );
        }

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add(
                "grant_type",
                "refresh_token"
        );

        form.add(
                "refresh_token",
                refreshToken
        );

        String raw =
                restClient.post()
                        .uri(
                                properties.getTokenUrl()
                        )
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED
                        )
                        .headers(
                                headers ->
                                        headers.setBasicAuth(
                                                properties.getClientId(),
                                                properties.getClientSecret(),
                                                StandardCharsets.UTF_8
                                        )
                        )
                        .body(form)
                        .retrieve()
                        .body(String.class);

        TokenResponse response =
                parseTokenResponse(raw);

        sessionStore.saveTokens(
                response.accessToken(),
                response.refreshToken(),
                response.expiresIn()
        );
    }

    private TokenResponse parseTokenResponse(
            String raw
    ) {

        try {

            JsonNode root =
                    jsonMapper.readTree(raw);

            String accessToken =
                    root.path(
                            "access_token"
                    ).asText();

            String refreshToken =
                    root.path(
                            "refresh_token"
                    ).asText();

            long expiresIn =
                    root.path(
                            "expires_in"
                    ).asLong(7200);

            if (accessToken == null
                    || accessToken.isBlank()) {

                throw new IllegalStateException(
                        "X did not return an access token: "
                                + raw
                );
            }

            return new TokenResponse(
                    accessToken,
                    refreshToken,
                    expiresIn
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to parse X OAuth token response",
                    exception
            );
        }
    }

    private void loadCurrentUser(
            String accessToken
    ) {

        String raw =
                restClient.get()
                        .uri(USER_ME_URL)
                        .headers(
                                headers ->
                                        headers.setBearerAuth(
                                                accessToken
                                        )
                        )
                        .retrieve()
                        .body(String.class);

        try {

            JsonNode data =
                    jsonMapper.readTree(raw)
                            .path("data");

            sessionStore.saveUser(
                    data.path("id")
                            .asText(),
                    data.path("username")
                            .asText(),
                    data.path("name")
                            .asText()
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to read connected X account",
                    exception
            );
        }
    }

    private String randomValue(
            int size
    ) {

        byte[] bytes =
                new byte[size];

        secureRandom.nextBytes(bytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String createChallenge(
            String verifier
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            verifier.getBytes(
                                    StandardCharsets.US_ASCII
                            )
                    );

            return Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to create PKCE challenge",
                    exception
            );
        }
    }

    private record TokenResponse(
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {
    }

    public record ConnectionStatus(
            boolean connected,
            String userId,
            String username,
            String name,
            Instant expiresAt,
            boolean refreshTokenAvailable
    ) {
    }
}