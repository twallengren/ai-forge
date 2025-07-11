package aiforge.agents;

import aiforge.ai.AIRequest;
import aiforge.ai.AIWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record Agent(String name, String purpose, LongTermMemory longTermMemory, ShortTermMemory shortTermMemory, AIWorker worker) {

    private static final AIRequest.StructuredFormat CREATE_TASK_FORMAT = new AIRequest.StructuredFormat()
            .addProperty("title", "string", true)
            .addProperty("description", "string", true)
            .addArrayProperty("detailedRequirements", "string", true);
    private static final AIRequest.StructuredFormat PERFORM_TASK_FORMAT = new AIRequest.StructuredFormat()
            .addArrayProperty("shortTermMemory", "string", true);

    private static final String GLOBAL_SYSTEM_PROMPT = loadGlobalSystemPrompt();
    private static final String TASK_GENERATION_PROMPT = "Generate a task to help achieve your purpose.";

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

    public static Agent create(String name, String purpose, AIWorker worker) {
        return new Agent(name, purpose, new LongTermMemory(), new ShortTermMemory(), worker);
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

            List<String> shortTermDetails = objectMapper.convertValue(
                    responseJson.get("shortTermMemory"), new TypeReference<>() {});

            // Store details in the respective memory stores
            shortTermDetails.forEach(detail -> shortTermMemory.storeMemory(new Memory(detail)));
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
        String additionalPrompt = String.format("You are an AGENT. Your purpose is: %s. \n", purpose);
        additionalPrompt += switch (status) {
            case GENERATING_TASK -> """
                    Generating a new task is a big responsibility and should serve to push the purpose forward in
                    new, meaningful, and creative ways. Be mindful of the memory provided and make sure that new tasks
                    serve to create new memories or enhance existing ones. We do not want to be creating tasks that
                    result in redundant memories. Remember, the task should be clear, concise, and actionable.
                    """;
            case PERFORMING_TASK -> """
                    Execute the task in the prompt while adhering to the system's guidelines.\s
                    Focus on completing the task fully and addressing all requirements.\s
                    Identify the key results and add those to your short term memory.
                    Your short term memory will be used to enhance future tasks.
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
