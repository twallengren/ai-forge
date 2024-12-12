package aiforge.ai;

import java.util.Map;

public record AIRequest(Map<String,Object> context, String prompt, String id) {

    public static AIRequest of(Map<String, Object> context, String prompt) {
        return new AIRequest(context, prompt, "");
    }

    public static AIRequest of(AIRequest request, String id) {
        return new AIRequest(request.context(), request.prompt(), id);
    }
}
