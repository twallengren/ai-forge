package aiforge.ai;

import java.util.Optional;

public interface AIWorker {
    /**
     * Submit a request to the AI worker.
     *
     * @param request The AIRequest object containing context and prompt.
     * @return The request ID
     */
    String submitRequest(AIRequest request);

    Optional<String> getResponse(String requestId);

    /**
     * Shut down the AI worker.
     */
    void shutdown();
}
