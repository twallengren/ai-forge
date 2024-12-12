package aiforge.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OpenAIWorker implements AIWorker {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final ConcurrentHashMap<String, String> responseStorage = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAIWorker(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String submitRequest(AIRequest request) {
        String requestId = UUID.randomUUID().toString();
        AIRequest requestWithId = AIRequest.of(request, requestId);
        new Thread(() -> {
            try {
                String response = callOpenAI(requestWithId);
                responseStorage.put(requestId, response);
            } catch (IOException e) {
                responseStorage.put(requestId, "Error: " + e.getMessage());
            }
        }).start();
        return requestId;
    }

    @Override
    public Optional<String> getResponse(String requestId) {
        if (responseStorage.containsKey(requestId)) {
            return Optional.of(responseStorage.remove(requestId));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void shutdown() {

    }

    private String callOpenAI(AIRequest request) throws IOException {
        OkHttpClient client = new OkHttpClient();
        return null;
    }
}
