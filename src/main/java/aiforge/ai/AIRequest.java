package aiforge.ai;


import java.util.ArrayList;
import java.util.List;

public record AIRequest(String system, String prompt, String id, StructuredFormat structuredFormat) {

    public static AIRequest of(String system, String prompt, StructuredFormat structuredFormat) {
        return new AIRequest(system, prompt, "", structuredFormat);
    }

    public static AIRequest of(AIRequest request, String id, StructuredFormat structuredFormat) {
        return new AIRequest(request.system(), request.prompt(), id, structuredFormat);
    }

    public static AIRequest of(String system, String prompt) {
        return new AIRequest(system, prompt, "", null);
    }

    public static AIRequest of(AIRequest request, String id) {
        return new AIRequest(request.system(), request.prompt(), id, request.structuredFormat());
    }

    public static class StructuredFormat {
        private final List<Property> properties = new ArrayList<>();

        public record Property(String name, String type, boolean required, Property items) {
        }

        public StructuredFormat addProperty(String name, String type, boolean required) {
            properties.add(new Property(name, type, required, null));
            return this;
        }

        public StructuredFormat addArrayProperty(String name, String itemType, boolean required) {
            properties.add(new Property(name, "array", required, new Property(null, itemType, false, null)));
            return this;
        }

        public List<Property> properties() {
            return properties;
        }
    }
}
