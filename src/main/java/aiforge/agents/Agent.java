package aiforge.agents;

import aiforge.ai.AIRequest;
import aiforge.ai.AIWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record Agent(String name, String purpose, Context context, LongTermMemory longTermMemory, ShortTermMemory shortTermMemory, AIWorker worker) {

    private static final AIRequest.StructuredFormat CREATE_TASK_FORMAT = new AIRequest.StructuredFormat()
            .addProperty("title", "string", true)
            .addProperty("description", "string", true)
            .addArrayProperty("detailedRequirements", "string", true);
    private static final AIRequest.StructuredFormat PERFORM_TASK_FORMAT = new AIRequest.StructuredFormat()
            .addArrayProperty("detailsToAddToShortTermMemory", "string", true)
            .addArrayProperty("detailsToAddToLongTermMemory", "string", true);

    private static final String GLOBAL_SYSTEM_PROMPT = loadGlobalSystemPrompt();
    private static final String TASK_GENERATION_PROMPT = "Generate a task to help achieve your purpose.";

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

    public static Agent create(String name, String purpose, Context context, AIWorker worker) {
        return new Agent(name, purpose, context, new LongTermMemory(), new ShortTermMemory(), worker);
    }

    public void run() {
        for (int i = 0; i < 5; i++) {
            Task task = createTask();
            LOGGER.atInfo().log("{}: Task generated: {}", name, task.description());
            performTask(task);
            LOGGER.atInfo().log("{}: Task performed successfully", name);
            LOGGER.atInfo().log("{}: Long-term memory:\n{}", name, longTermMemory.getAllMemoriesAsString());
            LOGGER.atInfo().log("{}: Short-term memory:\n{}", name, shortTermMemory.getAllMemoriesAsString());
        }
    }

    public Task createTask() {
        LOGGER.atInfo().log("{}: Generating a new task...", name);

        // Create and submit the AIRequest
        AIRequest aiRequest = AIRequest.of(getSystemPrompt(Status.GENERATING_TASK), TASK_GENERATION_PROMPT, CREATE_TASK_FORMAT);
        String requestId = worker.submitRequest(aiRequest);
        LOGGER.atInfo().log("{}: Create task request submitted to AI worker", name);

        // Poll the AI worker for a response
        String response = pollForResponse(requestId);

        return Task.fromJson(response);
    }

    public void performTask(Task task) {
        LOGGER.atInfo().log("{}: Performing task...", name);

        // Combine memories into the task prompt
        String enhancedPrompt = enhancePromptWithMemories(task.toPrompt());
        LOGGER.atInfo().log("{}: Task prompt enhanced with memories", name);
        LOGGER.atDebug().log("{}: Enhanced task prompt:\n{}", name, enhancedPrompt);

        // Create and submit the AIRequest
        AIRequest aiRequest = AIRequest.of(getSystemPrompt(Status.PERFORMING_TASK), enhancedPrompt, PERFORM_TASK_FORMAT);
        String requestId = worker.submitRequest(aiRequest);
        LOGGER.atInfo().log("{}: Complete task request submitted to AI worker", name);

        // Poll the AI worker for a response
        String response = pollForResponse(requestId);

        // Parse and update memories
        updateMemories(response);
    }

    private String enhancePromptWithMemories(String taskPrompt) {
        // Fetch short-term memory
        String shortTermContext = shortTermMemory.getAllMemoriesAsString();

        // Fetch relevant long-term memory (filtering logic can be customized)
        List<Memory> relevantLongTermMemories = longTermMemory.getMemoriesByKey(name);
        String longTermContext = relevantLongTermMemories.stream()
                .map(Memory::getMemory)
                .collect(Collectors.joining("\n"));

        // Append memories to the task prompt
        return String.format(
                """
                %s
                
                Context from Short-Term Memory:
                %s
                
                Context from Long-Term Memory:
                %s
                
                Instructions:
                - Complete the task and ensure all requirements are addressed.
                - Use the provided context to enhance your response.
                """,
                taskPrompt,
                shortTermContext.isEmpty() ? "None" : shortTermContext,
                longTermContext.isEmpty() ? "None" : longTermContext
        );
    }

    private void updateMemories(String response) {
        try {
            // Parse JSON response into long-term and short-term memory details
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response);

            List<String> longTermDetails = objectMapper.convertValue(
                    responseJson.get("detailsToAddToLongTermMemory"), new TypeReference<>() {});
            List<String> shortTermDetails = objectMapper.convertValue(
                    responseJson.get("detailsToAddToShortTermMemory"), new TypeReference<>() {});

            // Store details in the respective memory stores
            longTermDetails.forEach(detail -> longTermMemory.storeMemory(name, new Memory(detail)));
            shortTermDetails.forEach(detail -> shortTermMemory.storeMemory(new Memory(detail)));

            LOGGER.atInfo().log("{}: Memories updated. Long-term: {}, Short-term: {}",
                    name, longTermDetails.size(), shortTermDetails.size());
        } catch (Exception e) {
            LOGGER.error("{}: Failed to parse or update memories: {}", name, e.getMessage(), e);
        }
    }

    private String pollForResponse(String requestId) {
        Optional<String> response = Optional.empty();
        while (response.isEmpty()) {
            LOGGER.atInfo().log("{}: Polling AI worker for response...", name);
            response = worker.getResponse(requestId);
            if (response.isEmpty()) {
                LOGGER.atInfo().log("{}: No response yet, waiting 2 seconds before polling again...", name);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOGGER.error("Task generation thread interrupted", e);
                }
            }
        }
        return response.get();
    }

    private String getSystemPrompt(Status status) {
        return GLOBAL_SYSTEM_PROMPT + "\n\n" + getAdditionalSystemPrompt(status, purpose);
    }

    private static String loadGlobalSystemPrompt() {
        try {
            Path path = Paths.get(Agent.class.getClassLoader().getResource("aiforge/agents/system.txt").toURI());
            return Files.readString(path);
        } catch (Exception e) {
            LOGGER.error("Error loading global system prompt from resources: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load global system prompt", e);
        }
    }

    private static String getAdditionalSystemPrompt(Status status, String purpose) {
        String additionalPrompt = String.format("You are an AGENT. Your purpose is: %s. ", purpose);
        additionalPrompt += switch (status) {
            case GENERATING_TASK -> "";
            case PERFORMING_TASK -> """
                Execute the task in the prompt while adhering to the system's guidelines.\s
                Focus on completing the task fully and addressing all requirements.\s
                After completing the task:
                1. Identify key information that other agents would need (Long-Term Memory).
                2. Identify temporary details you need to remember for your own context (Short-Term Memory).
                Your responses should demonstrate that you have fully performed the task, with clear outputs or results.
                Remember, you are PERFORMING the task, not analyzing or restating it.
                \s""";
            default -> "";
        };
        return additionalPrompt;
    }

    enum Status {
        GENERATING_TASK,
        PERFORMING_TASK
    }
}
