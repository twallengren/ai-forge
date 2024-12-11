package aiforge.ai;

import java.util.Map;
import java.util.UUID;

public record AIRequest(Map<String,Object> context, String prompt, String id) {

    public static AIRequest of(Map<String, Object> context, String prompt) {
        return new AIRequest(context, prompt, UUID.randomUUID().toString());
    }
}
