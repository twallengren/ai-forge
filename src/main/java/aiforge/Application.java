package aiforge;

import aiforge.ai.AIRequest;
import aiforge.ai.AIWorker;
import aiforge.ai.OllamaWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {

        LOGGER.atInfo().log("Starting Llama AI worker...");
        AIWorker worker = new OllamaWorker("llama3");

        LOGGER.atInfo().log("Submitting request...");
        Map<String, String> context = new HashMap<>();
        context.put("character", "Eldor is a wise, secretive elf.");
        AIRequest request = new AIRequest(context, "Describe Eldor's appearance.");
        worker.submitRequest(request);

        // Allow processing before shutdown
        try {
            Thread.sleep(10000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        worker.shutdown();
    }
}
