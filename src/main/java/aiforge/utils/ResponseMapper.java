package aiforge.utils;

import aiforge.ai.AIResponse;

public class ResponseMapper {

    public static AIResponse mapToAIRequest(OllamaResponse ollamaResponse) {
        // Map back to AIRequest using response data
        return AIResponse.of(ollamaResponse.response());
    }
}
