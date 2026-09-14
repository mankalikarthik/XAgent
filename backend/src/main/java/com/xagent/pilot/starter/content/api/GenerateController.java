package com.xagent.pilot.starter.content.api;

import com.xagent.pilot.starter.ai.domain.LanguageModel;
import com.xagent.pilot.starter.ai.domain.LlmRequest;
import com.xagent.pilot.starter.ai.domain.LlmResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/generate")
public class GenerateController {

    private final LanguageModel languageModel;

    public GenerateController(LanguageModel languageModel) {
        this.languageModel = languageModel;
    }

    @PostMapping
    public LlmResponse generate(
            @RequestBody LlmRequest request
    ) {
        return languageModel.generate(request);
    }
}
