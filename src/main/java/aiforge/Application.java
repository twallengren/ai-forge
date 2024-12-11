package aiforge;

import aiforge.agents.Task;
import aiforge.ai.AIWorker;
import aiforge.ai.OllamaWorker;
import aiforge.agents.Agent;
import aiforge.agents.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {

        LOGGER.atInfo().log("Starting Llama AI worker...");
        AIWorker worker = new OllamaWorker("tinyllama");

        LOGGER.atInfo().log("Creating agent...");
        Context context = new Context(Map.of(
                "genre", "Fantasy",
                "worldSetting", Map.of(
                        "name", "Ancient Forest",
                        "features", Map.of(
                                "terrain", "Dense with towering trees and hidden clearings",
                                "climate", "Misty and cool, with magical glowing flora"
                        ),
                        "inhabitants", Map.of(
                                "sentientSpecies", List.of("Elves", "Fairies", "Dryads"),
                                "creatures", List.of("Unicorns", "Forest dragons", "Sprite swarms")
                        )
                ),
                "mainCharacter", Map.of(
                        "name", "Eldor",
                        "traits", List.of("Wise", "Secretive", "Compassionate"),
                        "age", "300 years",
                        "role", "Keeper of ancient magical knowledge",
                        "allies", List.of("The Seer of the Northern Glade", "Lyra the Fairy Queen"),
                        "backstory", "Eldor has lived for centuries, protecting the ancient forest from invaders and unraveling its mystical secrets."
                )
        ));
        String purpose = "Create a backstory for Eldor.";
        Agent agent = Agent.create("CharacterAgent", purpose, context, worker);
        LOGGER.atInfo().log("Agent created");

        LOGGER.atInfo().log("Creating task...");
        Task task = agent.createTask();

        LOGGER.atInfo().log("Task generated: {}", task.description());

        LOGGER.atInfo().log("Shutting down worker...");
        worker.shutdown();
    }
}
