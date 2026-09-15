package com.xagent.pilot.starter.content.infrastructure.x;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class XOAuthSessionStore {

    private final XAccountConnectionRepository repository;
    private final XTokenCryptoService cryptoService;

    /*
     * These two values are intentionally memory-only.
     *
     * They only exist while an OAuth authorization flow
     * is in progress.
     */
    private String state;
    private String codeVerifier;

    /*
     * Active account/session values.
     */
    private String accessToken;
    private String refreshToken;

    private Instant expiresAt;

    private String userId;
    private String username;
    private String name;

    public XOAuthSessionStore(
            XAccountConnectionRepository repository,
            XTokenCryptoService cryptoService
    ) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    /*
     * Restore the most recently connected X account
     * whenever Spring Boot starts.
     */
    @PostConstruct
    public synchronized void restorePersistedConnection() {

        repository
                .findTopByOrderByUpdatedAtDesc()
                .ifPresent(this::restoreFromEntity);
    }

    public synchronized void beginAuthorization(
            String state,
            String codeVerifier
    ) {

        this.state = state;
        this.codeVerifier = codeVerifier;
    }

    public synchronized String consumeCodeVerifier(
            String returnedState
    ) {

        if (state == null
                || !Objects.equals(
                state,
                returnedState
        )) {

            throw new IllegalStateException(
                    "Invalid OAuth state"
            );
        }

        String verifier = codeVerifier;

        state = null;
        codeVerifier = null;

        return verifier;
    }

    /*
     * Called both:
     *
     * 1. After initial OAuth authorization
     * 2. After access-token refresh
     */
    public synchronized void saveTokens(
            String accessToken,
            String refreshToken,
            long expiresInSeconds
    ) {

        if (accessToken == null
                || accessToken.isBlank()) {

            throw new IllegalArgumentException(
                    "Access token cannot be empty"
            );
        }

        this.accessToken = accessToken;

        /*
         * X may rotate the refresh token.
         *
         * If a refresh response doesn't contain a new
         * one, keep the existing refresh token.
         */
        if (refreshToken != null
                && !refreshToken.isBlank()) {

            this.refreshToken = refreshToken;
        }

        this.expiresAt =
                Instant.now()
                        .plusSeconds(
                                expiresInSeconds
                        );

        /*
         * During the first OAuth connection we don't
         * know the X user ID yet.
         *
         * saveUser() will persist everything once
         * /2/users/me returns.
         *
         * During a refresh, however, userId is already
         * known, so persist immediately.
         */
        if (userId != null
                && !userId.isBlank()) {

            persistConnection();
        }
    }

    /*
     * Called after /2/users/me.
     *
     * At this point we have both:
     *
     * - OAuth tokens
     * - X account identity
     *
     * so we can safely persist the connection.
     */
    public synchronized void saveUser(
            String userId,
            String username,
            String name
    ) {

        if (userId == null
                || userId.isBlank()) {

            throw new IllegalArgumentException(
                    "X user ID cannot be empty"
            );
        }

        this.userId = userId;
        this.username = username;
        this.name = name;

        persistConnection();
    }

    private void persistConnection() {

        if (accessToken == null
                || accessToken.isBlank()) {

            return;
        }

        if (expiresAt == null) {

            return;
        }

        if (userId == null
                || userId.isBlank()) {

            return;
        }

        String encryptedAccessToken =
                cryptoService.encrypt(
                        accessToken
                );

        String encryptedRefreshToken =
                cryptoService.encrypt(
                        refreshToken
                );

        Instant now =
                Instant.now();

        XAccountConnectionEntity entity =
                repository
                        .findByXUserId(
                                userId
                        )
                        .orElseGet(
                                () ->
                                        new XAccountConnectionEntity(
                                                UUID.randomUUID(),
                                                userId,
                                                username,
                                                name,
                                                encryptedAccessToken,
                                                encryptedRefreshToken,
                                                expiresAt,
                                                now,
                                                now
                                        )
                        );

        /*
         * Existing connection:
         * update user details and rotate tokens.
         */
        if (repository.existsById(
                entity.getId()
        )) {

            entity.updateUser(
                    username,
                    name
            );

            entity.updateTokens(
                    encryptedAccessToken,
                    encryptedRefreshToken,
                    expiresAt
            );
        }

        repository.save(entity);
    }

    private void restoreFromEntity(
            XAccountConnectionEntity entity
    ) {

        this.userId =
                entity.getXUserId();

        this.username =
                entity.getUsername();

        this.name =
                entity.getDisplayName();

        this.accessToken =
                cryptoService.decrypt(
                        entity.getAccessTokenEncrypted()
                );

        this.refreshToken =
                cryptoService.decrypt(
                        entity.getRefreshTokenEncrypted()
                );

        this.expiresAt =
                entity.getExpiresAt();
    }

    public synchronized String getAccessToken() {
        return accessToken;
    }

    public synchronized String getRefreshToken() {
        return refreshToken;
    }

    public synchronized Instant getExpiresAt() {
        return expiresAt;
    }

    public synchronized boolean isConnected() {

        return accessToken != null
                && !accessToken.isBlank()
                && userId != null
                && !userId.isBlank();
    }

    public synchronized boolean expiresSoon() {

        return expiresAt != null
                && Instant.now()
                .isAfter(
                        expiresAt.minusSeconds(
                                60
                        )
                );
    }

    public synchronized String getUserId() {
        return userId;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized String getName() {
        return name;
    }
}