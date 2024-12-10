package aiforge.ai;

public interface AIWorker {
    /**
     * Submit a request to the AI worker.
     *
     * @param request The AIRequest object containing context and prompt.
     */
    void submitRequest(AIRequest request);

    /**
     * Shut down the AI worker.
     */
    void shutdown();
}
