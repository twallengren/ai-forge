package aiforge.agents;

import aiforge.ai.AIRequest;
import aiforge.ai.AIWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public record Agent(String name, String purpose, Context context, Memory memory, AIWorker worker) {

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);
    private static String globalSystemPrompt;

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

        // Combine global system prompt with agent-specific instructions
        String agentSpecificPrompt = String.format(
                "You are an AGENT. Your purpose is: %s. You should identify tasks that fulfill this purpose while adhering to the system's guidelines.",
                purpose
        );
        String formattedPrompt = globalSystemPrompt + "\n\n" + agentSpecificPrompt;

        // Define the task generation prompt
        String taskPrompt = "Generate a task description based on the provided context and memory.";

        // Create and submit the AIRequest
        AIRequest aiRequest = AIRequest.of(formattedPrompt, taskPrompt);
        String requestId = worker.submitRequest(aiRequest);
        LOGGER.atInfo().log("{}: Create task request submitted to AI worker", name);

        // Poll the AI worker for a response
        Optional<String> response = Optional.empty();
        while (response.isEmpty()) {
            LOGGER.atInfo().log("{}: Polling AI worker for response...", name);
            response = worker.getResponse(requestId);
            if (response.isEmpty()) {
                LOGGER.atInfo().log("{}: No response yet, waiting 10 seconds before polling again...", name);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    LOGGER.error("Task generation thread interrupted", e);
                }
            }
        }

        return new Task(response.get(), context, memory);
    }

    public void performTask(Task task) {
        LOGGER.atInfo().log("{}: Performing task...", name);

        // Combine global system prompt with agent-specific instructions
        String agentSpecificPrompt = String.format(
                "You are an AGENT. Your purpose is: %s. Execute the following task while adhering to the system's guidelines.",
                purpose
        );
        String formattedPrompt = globalSystemPrompt + "\n\n" + agentSpecificPrompt;

        // The task description is the prompt
        String taskDescription = task.description();

        // Create and submit the AIRequest
        AIRequest aiRequest = AIRequest.of(formattedPrompt, taskDescription);
        String requestId = worker.submitRequest(aiRequest);
        LOGGER.atInfo().log("{}: Complete task request submitted to AI worker", name);

        // Poll the AI worker for a response
        Optional<String> response = Optional.empty();
        while (response.isEmpty()) {
            LOGGER.atInfo().log("{}: Polling AI worker for response...", name);
            response = worker.getResponse(requestId);
            if (response.isEmpty()) {
                LOGGER.atInfo().log("{}: No response yet, waiting 10 seconds before polling again...", name);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    LOGGER.error("Task completion thread interrupted", e);
                }
            }
        }

        // Append the response to memory
        memory.append(response.get());
        LOGGER.atInfo().log("{}: Task completed and memory updated: {}", name, response.get());
    }

    private static void loadGlobalSystemPrompt() {
        try {
            Path path = Paths.get(Agent.class.getClassLoader().getResource("aiforge/agents/system.txt").toURI());
            globalSystemPrompt = Files.readString(path);
        } catch (Exception e) {
            LOGGER.error("Error loading global system prompt from resources: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load global system prompt", e);
        }
    }

}
