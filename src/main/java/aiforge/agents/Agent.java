package aiforge.agents;

import aiforge.ai.AIRequest;
import aiforge.ai.AIWorker;
import aiforge.utils.OllamaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public record Agent(String name, String purpose, Context context, Memory memory, AIWorker worker) {

    private static final String GLOBAL_SYSTEM_PROMPT = loadGlobalSystemPrompt();
    private static final String TASK_GENERATION_PROMPT = "Generate a task to help achieve your purpose.";

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

    public static Agent create(String name, String purpose, Context context, AIWorker worker) {
        loadGlobalSystemPrompt();
        return new Agent(name, purpose, context, new Memory(), worker);
    }

    public void run() {
        Task task = createTask();
        LOGGER.atInfo().log("{}: Task generated: {}", name, task.description());
        performTask(task);
    }

    public Task createTask() {
        LOGGER.atInfo().log("{}: Generating a new task...", name);

        // Create and submit the AIRequest
        AIRequest.StructuredFormat structuredFormat = new AIRequest.StructuredFormat()
                .addProperty("TaskTitle", "string", true)
                .addProperty("TaskDescription", "string", true)
                .addProperty("TaskPriority", "integer", true)
                .addArrayProperty("DetailedRequirements", "string", true);
        AIRequest aiRequest = AIRequest.of(getSystemPrompt(Status.GENERATING_TASK), TASK_GENERATION_PROMPT, structuredFormat);
        String requestId = worker.submitRequest(aiRequest);
        LOGGER.atInfo().log("{}: Create task request submitted to AI worker", name);

        // Poll the AI worker for a response
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

        return new Task(response.get(), context, memory);
    }

    public void performTask(Task task) {
        LOGGER.atInfo().log("{}: Performing task...", name);

        // The task description is the prompt
        String taskDescription = task.description();

        // Create and submit the AIRequest
        AIRequest aiRequest = AIRequest.of(getSystemPrompt(Status.PERFORMING_TASK), taskDescription);
        String requestId = worker.submitRequest(aiRequest);
        LOGGER.atInfo().log("{}: Complete task request submitted to AI worker", name);

        // Poll the AI worker for a response
        Optional<String> response = Optional.empty();
        while (response.isEmpty()) {
            LOGGER.atInfo().log("{}: Polling AI worker for response...", name);
            response = worker.getResponse(requestId);
            if (response.isEmpty()) {
                LOGGER.atInfo().log("{}: No response yet, waiting 2 seconds before polling again...", name);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOGGER.error("Task completion thread interrupted", e);
                }
            }
        }

        // Append the response to memory
        memory.append(response.get());
        LOGGER.atInfo().log("{}: Task completed and memory updated: {}", name, response.get());
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
            case PERFORMING_TASK -> "Execute the task in the prompt while adhering to the system's guidelines.";
            default -> "";
        };
        return additionalPrompt;
    }

    enum Status {
        GENERATING_TASK,
        PERFORMING_TASK
    }

}
