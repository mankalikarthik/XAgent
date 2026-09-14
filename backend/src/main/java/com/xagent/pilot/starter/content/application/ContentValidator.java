package com.xagent.pilot.starter.content.application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ContentValidator {

    private static final int MAX_CHARACTERS = 280;
    private static final int MAX_LINES = 4;
    private static final int MAX_HASHTAGS = 3;

    private static final Pattern HASHTAG_PATTERN =
            Pattern.compile("(?<!\\w)#\\w+");

    public ValidationResult validate(String content) {

        List<String> reasons = new ArrayList<>();

        if (content == null || content.isBlank()) {
            reasons.add("Content is blank");

            return new ValidationResult(
                    false,
                    reasons
            );
        }

        int characterCount =
                content.codePointCount(
                        0,
                        content.length()
                );

        if (characterCount > MAX_CHARACTERS) {
            reasons.add(
                    "Content exceeds 280 characters: "
                            + characterCount
            );
        }

        long lineCount = content.lines().count();

        if (lineCount > MAX_LINES) {
            reasons.add(
                    "Too many lines: " + lineCount
            );
        }

        Matcher matcher =
                HASHTAG_PATTERN.matcher(content);

        int hashtags = 0;

        while (matcher.find()) {
            hashtags++;
        }

        if (hashtags > MAX_HASHTAGS) {
            reasons.add(
                    "Too many hashtags: " + hashtags
            );
        }

        return new ValidationResult(
                reasons.isEmpty(),
                reasons
        );
    }

    public record ValidationResult(
            boolean valid,
            List<String> reasons
    ) {
    }
}