package aiforge.ai;

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

public class OllamaWorker extends QueueBasedAIWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaWorker.class);

    private final ChatLanguageModel chatLanguageModel;
    private final GenericContainer<?> ollamaContainer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaWorker(String modelName) {
        LOGGER.atInfo().log("Initializing OllamaWorker for model: {}", modelName);
        DockerImageName dockerImageName = DockerImageName.parse("langchain4j/ollama-" + modelName + ":latest")
                .asCompatibleSubstituteFor("ollama/ollama");
        this.ollamaContainer = new OllamaContainer(dockerImageName);
        this.ollamaContainer.start();
        String baseUrl = String.format("http://%s:%d", ollamaContainer.getHost(), ollamaContainer.getFirstMappedPort());
        LOGGER.atInfo().log("OllamaContainer started at: {}", baseUrl);
        this.chatLanguageModel = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.8)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    @Override
    protected String processRequest(AIRequest request) {
        LOGGER.atInfo().log("Processing request: {}", request);
        String contextJson = "";
        try {
            contextJson = objectMapper.writeValueAsString(request.context());
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting context to JSON", e);
        }
        SystemMessage systemMessage = SystemMessage.from(contextJson);
        UserMessage userMessage = UserMessage.from(request.prompt());
        Response<AiMessage> response = chatLanguageModel.generate(systemMessage, userMessage);
        return response.content().text();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        ollamaContainer.stop();
    }
}
