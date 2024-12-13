package aiforge.ai;

public record AIResponse(String response, String id) {
    public static AIResponse of(String response) {
        return new AIResponse(response, "");
    }

    public static AIResponse of(String response, String id) {
        return new AIResponse(response, id);
    }
}
