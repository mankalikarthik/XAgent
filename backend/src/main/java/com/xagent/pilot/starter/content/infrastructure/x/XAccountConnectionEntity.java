package com.xagent.pilot.starter.content.infrastructure.x;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "x_account_connection")
public class XAccountConnectionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "x_user_id",
            nullable = false
    )
    private String xUserId;

    @Column(
            name = "username",
            nullable = false
    )
    private String username;

    @Column(name = "display_name")
    private String displayName;

    @Column(
            name = "access_token_encrypted",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String accessTokenEncrypted;

    @Column(
            name = "refresh_token_encrypted",
            columnDefinition = "TEXT"
    )
    private String refreshTokenEncrypted;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private Instant expiresAt;

    @Column(
            name = "connected_at",
            nullable = false
    )
    private Instant connectedAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected XAccountConnectionEntity() {
    }

    public XAccountConnectionEntity(
            UUID id,
            String xUserId,
            String username,
            String displayName,
            String accessTokenEncrypted,
            String refreshTokenEncrypted,
            Instant expiresAt,
            Instant connectedAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.xUserId = xUserId;
        this.username = username;
        this.displayName = displayName;
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.refreshTokenEncrypted = refreshTokenEncrypted;
        this.expiresAt = expiresAt;
        this.connectedAt = connectedAt;
        this.updatedAt = updatedAt;
    }

    public void updateTokens(
            String accessTokenEncrypted,
            String refreshTokenEncrypted,
            Instant expiresAt
    ) {
        this.accessTokenEncrypted =
                accessTokenEncrypted;

        if (refreshTokenEncrypted != null
                && !refreshTokenEncrypted.isBlank()) {

            this.refreshTokenEncrypted =
                    refreshTokenEncrypted;
        }

        this.expiresAt =
                expiresAt;

        this.updatedAt =
                Instant.now();
    }

    public void updateUser(
            String username,
            String displayName
    ) {
        this.username =
                username;

        this.displayName =
                displayName;

        this.updatedAt =
                Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getXUserId() {
        return xUserId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAccessTokenEncrypted() {
        return accessTokenEncrypted;
    }

    public String getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}