package aiforge.utils;

import aiforge.ai.AIRequest;

public class RequestMapper {

    public static OllamaRequest mapToOllamaRequest(AIRequest aiRequest) {

        return new OllamaRequest.Builder()
                .model(OllamaRequest.Model.LLAMA3P1) // Default to LLAMA3P2 or customize based on context
                .prompt(aiRequest.prompt())
                .system(aiRequest.system())
                .stream(false) // Default setting, customize as needed
                .build();
    }
}
