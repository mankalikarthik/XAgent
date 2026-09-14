package com.xagent.pilot.starter.content.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record GenerateCandidatesRequest(

        @NotBlank
        String topic,

        @NotBlank
        String tone,

        @Min(1)
        @Max(5)
        int count

) {
}