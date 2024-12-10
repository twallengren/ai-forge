package aiforge.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Abstract class providing high-level queue-based request handling for AI workers.
 */
public abstract class QueueBasedAIWorker implements AIWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueBasedAIWorker.class);

    private final BlockingQueue<AIRequest> requestQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public QueueBasedAIWorker() {
        LOGGER.atInfo().log("Initializing QueueBasedAIWorker...");
        executor.submit(this::processQueue);
    }

    @Override
    public void submitRequest(AIRequest request) {
        LOGGER.atInfo().log("Submitting request to the queue: {}", request);
        requestQueue.add(request);
    }

    @Override
    public void shutdown() {
        LOGGER.atInfo().log("Shutting down QueueBasedAIWorker...");
        executor.shutdownNow();
    }

    /**
     * Process the request queue, calling the abstract `processRequest` method for each request.
     */
    private void processQueue() {
        try {
            while (true) {
                LOGGER.atInfo().log("Waiting to take a request from the queue...");
                AIRequest request = requestQueue.take();
                LOGGER.atInfo().log("Processing request: {}", request);
                String response = processRequest(request);
                handleResponse(response);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.atInfo().log("Worker interrupted, shutting down.");
        }
    }

    /**
     * Abstract method to be implemented by subclasses for processing a single request.
     *
     * @param request The AIRequest object.
     * @return The response as a String.
     */
    protected abstract String processRequest(AIRequest request);

    /**
     * Hook for handling responses after processing.
     *
     * @param response The response string.
     */
    protected void handleResponse(String response) {
        LOGGER.atInfo().log("Response: {}", response);
    }
}