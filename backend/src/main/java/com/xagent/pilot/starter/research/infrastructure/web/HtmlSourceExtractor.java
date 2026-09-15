package com.xagent.pilot.starter.research.infrastructure.web;

import com.xagent.pilot.starter.research.domain.FetchedPage;
import com.xagent.pilot.starter.research.domain.SourceDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class HtmlSourceExtractor {

    private static final int MAX_TEXT_LENGTH = 100_000;

    /*
     * Only trust an end-marker if we've already extracted
     * a reasonable amount of article text.
     */
    private static final int MIN_TEXT_BEFORE_END_MARKER = 200;

    /*
     * Ordered from most specific to most generic.
     *
     * News sites use wildly different HTML structures,
     * so we try common article-body conventions before
     * falling back to article/main/body.
     */
    private static final List<String> CONTENT_SELECTORS =
            List.of(
                    "[itemprop=articleBody]",
                    "[data-testid=article-body]",
                    ".article-body",
                    ".articleBody",
                    ".article-content",
                    ".article_content",
                    ".story-body",
                    ".storyBody",
                    ".story-content",
                    ".story_content",
                    ".content__article-body",
                    "article",
                    "main"
            );

    /*
     * These elements commonly contain page chrome rather
     * than evidence relevant to the article itself.
     */
    private static final String NOISE_SELECTORS = String.join(
            ", ",
            "script",
            "style",
            "noscript",
            "svg",
            "canvas",
            "iframe",
            "form",
            "nav",
            "footer",
            "aside",
            "[hidden]",

            ".advertisement",
            ".advertisements",
            ".ads",
            ".ad-container",

            ".social-share",
            ".share",
            ".share-bar",

            ".newsletter",
            ".subscription",
            ".subscribe",

            ".comments",
            ".comment-section",

            ".poll",
            ".poll-container",

            ".related",
            ".related-stories",
            ".related-content",

            ".recommended",
            ".recommendations",

            ".trending",
            ".trending-stories",

            ".most-read",
            ".popular",
            ".hot-picks",

            ".breadcrumb",
            ".breadcrumbs",

            "[class*=related]",
            "[id*=related]",

            "[class*=recommend]",
            "[id*=recommend]",

            "[class*=trending]",
            "[id*=trending]",

            "[class*=newsletter]",
            "[id*=newsletter]",

            "[class*=subscribe]",
            "[id*=subscribe]",

            "[class*=advert]",
            "[id*=advert]",

            "[class*=social]",
            "[id*=social]",

            "[class*=share]",
            "[id*=share]",

            "[class*=poll]",
            "[id*=poll]"
    );

    /*
     * Some sites put recommendation widgets inside the
     * same DOM container as the article, making CSS
     * cleanup insufficient.
     *
     * These markers let us stop once the actual article
     * has clearly ended.
     */
    private static final List<String> END_MARKERS =
            List.of(
                    "end of article",
                    "catch the latest",
                    "follow us on social media",
                    "trending stories",
                    "hot picks",
                    "daily puzzles",
                    "explore more puzzles",
                    "photostories",
                    "popular categories",
                    "about us",
                    "copyright ©"
            );

    public SourceDocument extract(
            FetchedPage page
    ) {

        try {

            Document document;

            try (ByteArrayInputStream input =
                         new ByteArrayInputStream(
                                 page.body()
                         )) {

                document = Jsoup.parse(
                        input,
                        null,
                        page.finalUri().toString()
                );
            }

            /*
             * Remove obvious site chrome before we even
             * choose the article container.
             */
            document
                    .select(NOISE_SELECTORS)
                    .remove();

            String title =
                    document.title();

            if (title == null
                    || title.isBlank()) {

                title =
                        page.finalUri()
                                .getHost();
            }

            Element root =
                    findContentRoot(
                            document
                    );

            if (root == null) {

                throw new IllegalStateException(
                        "HTML document contains no readable body"
                );
            }

            /*
             * Clone it so further cleanup does not modify
             * the original parsed document.
             */
            Element cleanedRoot =
                    root.clone();

            cleanedRoot
                    .select(NOISE_SELECTORS)
                    .remove();

            String text =
                    normalize(
                            cleanedRoot.text()
                    );

            text =
                    trimBoilerplate(
                            text
                    );

            if (text.isBlank()) {

                throw new IllegalStateException(
                        "No readable content extracted"
                );
            }

            if (text.length()
                    > MAX_TEXT_LENGTH) {

                text =
                        text.substring(
                                0,
                                MAX_TEXT_LENGTH
                        );
            }

            return new SourceDocument(
                    UUID.randomUUID(),
                    page.requestedUri()
                            .toString(),
                    page.finalUri()
                            .toString(),
                    title.trim(),
                    text,
                    sha256(text),
                    page.contentType(),
                    page.fetchedAt()
            );

        } catch (Exception exception) {

            if (exception
                    instanceof IllegalStateException) {

                throw (IllegalStateException)
                        exception;
            }

            throw new IllegalStateException(
                    "Unable to extract HTML source",
                    exception
            );
        }
    }

    private Element findContentRoot(
            Document document
    ) {

        for (String selector
                : CONTENT_SELECTORS) {

            Element element =
                    document.selectFirst(
                            selector
                    );

            if (element != null
                    && !element.text()
                    .isBlank()) {

                return element;
            }
        }

        return document.body();
    }

    private String trimBoilerplate(
            String text
    ) {

        String lower =
                text.toLowerCase(
                        Locale.ROOT
                );

        int cutIndex =
                text.length();

        for (String marker
                : END_MARKERS) {

            int markerIndex =
                    lower.indexOf(
                            marker
                    );

            if (markerIndex
                    >= MIN_TEXT_BEFORE_END_MARKER
                    && markerIndex
                    < cutIndex) {

                cutIndex =
                        markerIndex;
            }
        }

        if (cutIndex
                < text.length()) {

            return text
                    .substring(
                            0,
                            cutIndex
                    )
                    .trim();
        }

        return text;
    }

    private String normalize(
            String text
    ) {

        return text
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private String sha256(
            String value
    ) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        byte[] hash =
                digest.digest(
                        value.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        return HexFormat
                .of()
                .formatHex(hash);
    }
}