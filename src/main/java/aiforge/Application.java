package aiforge;

import aiforge.ai.AIWorker;
import aiforge.agents.Agent;
import aiforge.agents.Context;
import aiforge.ai.LocalOllamaWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {

        LOGGER.atInfo().log("Starting Llama AI worker...");
        AIWorker worker = new LocalOllamaWorker();

        LOGGER.atInfo().log("Creating agent...");
        String purpose = "Develop a set of characters for a book about a community of talking catfish in the mississippi river";
        Agent agent = Agent.create("WorldBuildingAgent", purpose, worker);
        LOGGER.atInfo().log("Agent created");

        LOGGER.atInfo().log("Running...");
        agent.run();

        LOGGER.atInfo().log("Shutting down worker...");
        worker.shutdown();
    }
}
