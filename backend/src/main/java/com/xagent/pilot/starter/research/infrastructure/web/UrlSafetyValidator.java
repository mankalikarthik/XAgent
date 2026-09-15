package com.xagent.pilot.starter.research.infrastructure.web;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;

@Component
public class UrlSafetyValidator {

    public URI validate(String rawUrl) {

        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL cannot be blank");
        }

        URI uri = URI.create(rawUrl.trim());

        String scheme = uri.getScheme();

        if (!"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(
                    "Only HTTP and HTTPS URLs are allowed"
            );
        }

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(
                    "URL must contain a valid host"
            );
        }

        try {

            for (InetAddress address :
                    InetAddress.getAllByName(host)) {

                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()) {

                    throw new IllegalArgumentException(
                            "Private or local URLs are not allowed"
                    );
                }
            }

        } catch (IllegalArgumentException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to resolve URL host",
                    exception
            );
        }

        return uri.normalize();
    }
}