package aiforge.ai;

import aiforge.utils.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class LocalOllamaWorker extends QueueBasedAIWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalOllamaWorker.class);

    @Override
    protected AIResponse processRequest(AIRequest request) {
        LOGGER.atInfo().log("Processing request: {}", request.prompt());
        OllamaRequest ollamaRequest = RequestMapper.mapToOllamaRequest(request);
        try {
            OllamaResponse ollamaResponse = OllamaApiClient.sendRequest(ollamaRequest);
            return ResponseMapper.mapToAIRequest(ollamaResponse);
        } catch (Exception e) {
            LOGGER.atError().setCause(e).log("Failed to process request: {}", request.prompt());
            return AIResponse.of("Failed to process request");
        }
    }
}
