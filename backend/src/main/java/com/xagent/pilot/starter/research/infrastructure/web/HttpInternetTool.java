package com.xagent.pilot.starter.research.infrastructure.web;

import com.xagent.pilot.starter.research.domain.FetchedPage;
import com.xagent.pilot.starter.research.port.InternetTool;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Component
public class HttpInternetTool implements InternetTool {

    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_DOWNLOAD_BYTES = 2_000_000;

    private final UrlSafetyValidator urlSafetyValidator;
    private final HttpClient httpClient;

    public HttpInternetTool(
            UrlSafetyValidator urlSafetyValidator
    ) {
        this.urlSafetyValidator = urlSafetyValidator;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public FetchedPage fetch(URI requestedUri) {

        URI original =
                urlSafetyValidator.validate(
                        requestedUri.toString()
                );

        URI current = original;

        for (int redirect = 0;
             redirect <= MAX_REDIRECTS;
             redirect++) {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(current)
                            .timeout(Duration.ofSeconds(20))
                            .header(
                                    "User-Agent",
                                    "XAgentResearch/0.1"
                            )
                            .header(
                                    "Accept",
                                    "text/html,application/xhtml+xml"
                            )
                            .GET()
                            .build();

            try {

                HttpResponse<InputStream> response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofInputStream()
                        );

                int status = response.statusCode();

                if (status >= 300 && status < 400) {

                    String location =
                            response.headers()
                                    .firstValue("location")
                                    .orElseThrow(
                                            () -> new IllegalStateException(
                                                    "Redirect without Location header"
                                            )
                                    );

                    URI redirected =
                            current.resolve(location);

                    current =
                            urlSafetyValidator.validate(
                                    redirected.toString()
                            );

                    response.body().close();

                    continue;
                }

                if (status < 200 || status >= 300) {

                    response.body().close();

                    throw new IllegalStateException(
                            "HTTP request failed with status "
                                    + status
                    );
                }

                String contentType =
                        response.headers()
                                .firstValue("content-type")
                                .orElse(
                                        "application/octet-stream"
                                );

                String lower =
                        contentType.toLowerCase();

                if (!lower.contains("text/html")
                        && !lower.contains(
                                "application/xhtml+xml"
                        )) {

                    response.body().close();

                    throw new IllegalStateException(
                            "Unsupported content type: "
                                    + contentType
                    );
                }

                byte[] body;

                try (InputStream input =
                             response.body()) {

                    body =
                            input.readNBytes(
                                    MAX_DOWNLOAD_BYTES + 1
                            );
                }

                if (body.length
                        > MAX_DOWNLOAD_BYTES) {

                    throw new IllegalStateException(
                            "Source exceeds maximum download size"
                    );
                }

                return new FetchedPage(
                        original,
                        current,
                        status,
                        contentType,
                        body,
                        Instant.now()
                );

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                        "HTTP request interrupted",
                        exception
                );

            } catch (Exception exception) {

                if (exception
                        instanceof IllegalStateException) {

                    throw (IllegalStateException)
                            exception;
                }

                throw new IllegalStateException(
                        "Unable to fetch source: "
                                + current,
                        exception
                );
            }
        }

        throw new IllegalStateException(
                "Too many redirects"
        );
    }
}