package com.xagent.pilot.starter.ai.domain;

public interface LanguageModel {

    LlmResponse generate(LlmRequest request);

}