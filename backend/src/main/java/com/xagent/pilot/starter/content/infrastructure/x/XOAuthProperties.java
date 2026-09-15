package com.xagent.pilot.starter.content.infrastructure.x;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "xagent.x.oauth")
public class XOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizeUrl;
    private String tokenUrl;

    private List<String> scopes =
            new ArrayList<>();

    public String getClientId() {
        return clientId;
    }

    public void setClientId(
            String clientId
    ) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(
            String clientSecret
    ) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(
            String redirectUri
    ) {
        this.redirectUri = redirectUri;
    }

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public void setAuthorizeUrl(
            String authorizeUrl
    ) {
        this.authorizeUrl = authorizeUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(
            String tokenUrl
    ) {
        this.tokenUrl = tokenUrl;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(
            List<String> scopes
    ) {
        this.scopes = scopes;
    }
}