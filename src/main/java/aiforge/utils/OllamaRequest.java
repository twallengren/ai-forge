package aiforge.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public record OllamaRequest(
        String model,
        String prompt,
        String suffix,
        String format,
        String options,
        String system,
        String template,
        boolean stream,
        String raw,
        String keepAlive
) {

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonObject = mapper.createObjectNode();

            if (model != null) jsonObject.put("model", model);
            if (prompt != null) jsonObject.put("prompt", prompt);
            if (suffix != null) jsonObject.put("suffix", suffix);
            if (format != null) {
                try {
                    jsonObject.set("format", mapper.readTree(format));
                } catch (Exception e) {
                    throw new RuntimeException("Invalid format JSON", e);
                }
            }
            if (options != null) jsonObject.put("options", options);
            if (system != null) jsonObject.put("system", system);
            if (template != null) jsonObject.put("template", template);
            jsonObject.put("stream", stream);
            if (raw != null) jsonObject.put("raw", raw);
            if (keepAlive != null) jsonObject.put("keepAlive", keepAlive);

            return mapper.writeValueAsString(jsonObject);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize OllamaRequest to JSON", e);
        }
    }

    public enum Model {

        LLAMA3P1("llama3.1"),
        LLAMA3P2("llama3.2:3b");

        private final String value;
        Model(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public static class StructuredFormat {
        private final List<Property> properties = new ArrayList<>();

        public static class Property {
            private final String name;
            private final String type;
            private final boolean required;
            private final Property items;

            public Property(String name, String type, boolean required, Property items) {
                this.name = name;
                this.type = type;
                this.required = required;
                this.items = items;
            }

            public String toJson() {
                StringBuilder json = new StringBuilder("{");
                json.append("\"type\":\"").append(type).append("\"");
                if (items != null) {
                    json.append(",\"items\":").append(items.toJson());
                }
                json.append("}");
                return json.toString();
            }
        }

        public StructuredFormat addProperty(String name, String type, boolean required) {
            properties.add(new Property(name, type, required, null));
            return this;
        }

        public StructuredFormat addArrayProperty(String name, String itemType, boolean required) {
            properties.add(new Property(name, "array", required, new Property(null, itemType, false, null)));
            return this;
        }

        public String toJson() {
            StringBuilder json = new StringBuilder("{\"type\":\"object\",\"properties\":{");
            for (Property property : properties) {
                json.append("\"").append(property.name).append("\":").append(property.toJson()).append(",");
            }
            if (!properties.isEmpty()) {
                json.deleteCharAt(json.length() - 1);
            }
            json.append("},\"required\":[");
            for (Property property : properties) {
                if (property.required) {
                    json.append("\"").append(property.name).append("\", ");
                }
            }
            if (json.charAt(json.length() - 1) == ' ') {
                json.delete(json.length() - 2, json.length());
            }
            json.append("]}");
            return json.toString();
        }
    }

    public static class Builder {

        private String model;
        private String prompt;
        private String suffix;
        private String format;
        private String options;
        private String system;
        private String template;
        private boolean stream;
        private String raw;
        private String keepAlive;

        public Builder model(Model model) {
            this.model = model.value();
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder suffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder structuredFormat(StructuredFormat structuredFormat) {
            this.format = structuredFormat.toJson();
            return this;
        }

        public Builder options(String options) {
            this.options = options;
            return this;
        }

        public Builder system(String system) {
            this.system = system;
            return this;
        }

        public Builder template(String template) {
            this.template = template;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder raw(String raw) {
            this.raw = raw;
            return this;
        }

        public Builder keepAlive(String keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public OllamaRequest build() {
            return new OllamaRequest(model, prompt, suffix, format, options, system, template, stream, raw, keepAlive);
        }
    }
}
