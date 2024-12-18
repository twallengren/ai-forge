package aiforge.utils;

import aiforge.ai.AIRequest;

public class RequestMapper {

    public static OllamaRequest mapToOllamaRequest(AIRequest aiRequest) {

        AIRequest.StructuredFormat structuredFormat = aiRequest.structuredFormat();
        if (structuredFormat != null) {
            OllamaRequest.StructuredFormat ollamaStructuredFormat = new OllamaRequest.StructuredFormat();
            for (AIRequest.StructuredFormat.Property property : structuredFormat.properties()) {
                if (property.items() != null) {
                    ollamaStructuredFormat.addArrayProperty(property.name(), property.items().type(), property.required());
                } else {
                    ollamaStructuredFormat.addProperty(property.name(), property.type(), property.required());
                }
            }
            return new OllamaRequest.Builder()
                    .model(OllamaRequest.Model.LLAMA3P1) // Default to LLAMA3P1 or customize based on context
                    .prompt(aiRequest.prompt())
                    .system(aiRequest.system())
                    .structuredFormat(ollamaStructuredFormat)
                    .stream(false) // Default setting, customize as needed
                    .build();
        }

        return new OllamaRequest.Builder()
                .model(OllamaRequest.Model.LLAMA3P1) // Default to LLAMA3P2 or customize based on context
                .prompt(aiRequest.prompt())
                .system(aiRequest.system())
                .stream(false) // Default setting, customize as needed
                .build();
    }
}
