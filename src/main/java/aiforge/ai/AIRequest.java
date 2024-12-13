package aiforge.ai;

import java.util.Map;

public record AIRequest(String system, String prompt, String id) {

    public static AIRequest of(String system, String prompt) {
        return new AIRequest(system, prompt, "");
    }

    public static AIRequest of(AIRequest request, String id) {
        return new AIRequest(request.system(), request.prompt(), id);
    }
}
