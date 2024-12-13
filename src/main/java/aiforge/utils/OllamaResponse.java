package aiforge.utils;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OllamaResponse(
        String model,
        String created_at,
        String response,
        boolean done,
        @JsonProperty("done_reason") String doneReason,
        List<Integer> context,
        @JsonProperty("total_duration") long totalDuration,
        @JsonProperty("load_duration") long loadDuration,
        @JsonProperty("prompt_eval_count") int promptEvalCount,
        @JsonProperty("prompt_eval_duration") long promptEvalDuration,
        @JsonProperty("eval_count") int evalCount,
        @JsonProperty("eval_duration") long evalDuration
) {
}
