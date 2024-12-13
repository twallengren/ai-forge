package aiforge.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class QueueBasedAIWorker implements AIWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueBasedAIWorker.class);

    private final BlockingQueue<AIRequest> requestQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String,String> responseMap = new HashMap<>();

    public QueueBasedAIWorker() {
        LOGGER.atInfo().log("Initializing QueueBasedAIWorker...");
        executor.submit(this::processQueue);
    }

    @Override
    public String submitRequest(AIRequest request) {
        LOGGER.atInfo().log("Submitting request to the queue: {}", request.prompt());
        String requestId = UUID.randomUUID().toString();
        AIRequest requestWithId = AIRequest.of(request, requestId);
        requestQueue.add(requestWithId);
        return requestId;
    }

    @Override
    public void shutdown() {
        LOGGER.atInfo().log("Shutting down QueueBasedAIWorker...");
        executor.shutdownNow();
    }

    @Override
    public Optional<String> getResponse(String requestId) {
        if (responseMap.containsKey(requestId)) {
            return Optional.of(responseMap.remove(requestId));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Process the request queue, calling the abstract `processRequest` method for each request.
     */
    private void processQueue() {
        try {
            while (true) {
                LOGGER.atInfo().log("Waiting to take a request from the queue...");
                AIRequest request = requestQueue.take();
                LOGGER.atInfo().log("Processing request: {}", request.prompt());
                AIResponse response = processRequest(request);
                responseMap.put(request.id(), response.response());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.atInfo().log("Worker interrupted, shutting down.");
        }
    }

    protected abstract AIResponse processRequest(AIRequest request);
}