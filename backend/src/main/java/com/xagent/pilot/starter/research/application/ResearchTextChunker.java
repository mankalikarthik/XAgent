package com.xagent.pilot.starter.research.application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResearchTextChunker {

    private static final int CHUNK_SIZE = 6000;
    private static final int OVERLAP = 500;

    public List<String> chunk(String text) {

        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;

        while (start < text.length()) {

            int end = Math.min(
                    start + CHUNK_SIZE,
                    text.length()
            );

            chunks.add(
                    text.substring(start, end)
            );

            if (end == text.length()) {
                break;
            }

            start = end - OVERLAP;
        }

        return chunks;
    }
}