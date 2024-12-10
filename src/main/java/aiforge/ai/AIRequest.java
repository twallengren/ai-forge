package aiforge.ai;

import java.util.Map;

public record AIRequest(Map<String,String> context, String prompt) {
}
