package aiforge.agents;

import aiforge.ai.AIRequest;
import aiforge.ai.AIWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record Agent(String name, String purpose, Context context, Memory memory, AIWorker worker) {

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

    public static Agent create(String name, String purpose, Context context, AIWorker worker) {
        return new Agent(name, purpose, context, new Memory(), worker);
    }

    public void run() {
        Task task = createTask();
        LOGGER.atInfo().log("{}: Task generated: {}", name, task.description());
        performTask(task);
    }

    public Task createTask() {
        LOGGER.atInfo().log("{}: Generating a new task...", name);

        // Combine agent context, memory, and a system prompt into the request context
        Map<String, Object> requestContext = new HashMap<>();
        requestContext.put("context", context.data());
        requestContext.put("memory", memory.getMemory());
        requestContext.put("system_prompt", String.format(
                "I am an agent tasked with the following purpose: '%s'.\n\n" +
                        "You are a highly capable and creative AI assistant specializing in generating actionable tasks to help me achieve my purpose. Your primary goal is to design tasks that are:\n\n" +
                        "1. Directly aligned with my purpose and relevant to the provided context and memory.\n" +
                        "2. Well-structured, clearly defined, and easy to understand.\n" +
                        "3. Focused on achieving specific objectives that contribute to my overall mission.\n\n" +
                        "When generating a task, adhere to the following guidelines:\n" +
                        "- **Task Title**: Provide a concise, engaging title for the task.\n" +
                        "- **Objective**: Clearly state the primary goal of the task.\n" +
                        "- **Requirements**: List the specific actions, steps, or areas of focus required to complete the task. Be precise and logical, incorporating details from the context and memory.\n" +
                        "- **Deliverable**: Describe the expected outcome or result of the task in detail.\n" +
                        "You must ensure that:\n" +
                        "- The task is creative and logical, staying consistent with the context and memory provided.\n" +
                        "- The task does not repeat previously completed objectives or overlap unnecessarily.\n" +
                        "- The description avoids irrelevant details or tangents that do not serve the task's purpose.\n\n" +
                        "Your response should follow this exact structure:\n\n" +
                        "**Task Title:** [Insert task title here]\n\n" +
                        "**Objective:** [Insert task objective here]\n\n" +
                        "**Requirements:**\n" +
                        "1. [First requirement]\n" +
                        "2. [Second requirement]\n" +
                        "3. [Additional requirements as needed]\n\n" +
                        "**Deliverable:** [Detailed description of the expected output or result]\n\n" +
                        "Use the provided context, memory, and purpose as the foundation for designing the task. If the context or memory includes specific details (e.g., characters, settings, themes), ensure these are integrated thoughtfully into the task.",
                purpose
        ));

        // Define the task generation prompt
        String prompt = "Generate a task description based on the provided context and memory.";

        // Create and submit the AIRequest
        AIRequest aiRequest = AIRequest.of(requestContext, prompt);
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

        // Combine agent context, memory, and a system prompt into the request context
        Map<String, Object> requestContext = new HashMap<>();
        requestContext.put("context", context.data());
        requestContext.put("memory", memory.getMemory());
        requestContext.put("system_prompt", String.format(
                "I am an agent tasked with the following purpose: '%s'.\n\n" +
                        "You are a highly capable and creative AI assistant specializing in generating detailed, actionable content to help me achieve my purpose. " +
                        "Your task is to respond with updates that directly address the prompt provided below, formatted to be appended to my memory.\n\n" +
                        "Requirements:\n" +
                        "1. Be concise and easy to read, formatted as a bulleted list.\n" +
                        "2. Focus only on information relevant to the task.\n" +
                        "3. Avoid repetition of previously provided memory updates.\n" +
                        "4. Maintain consistency with the provided context and memory.\n\n" +
                        "Output format:\n" +
                        "- **Memory Updates**: A bulleted list of new information to append to the agent's memory.",
                purpose
        ));

        // The task description is the prompt
        String prompt = task.description();

        // Create and submit the AIRequest
        AIRequest aiRequest = AIRequest.of(requestContext, prompt);
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

}
