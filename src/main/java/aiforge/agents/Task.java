package aiforge.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;

public record Task(String title, String description, List<String> detailedRequirements) {

    public static Task fromJson(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, Task.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON into Task", e);
        }
    }

    public String toPrompt() {
        return String.format(
                """
                TASK TITLE: %s
                TASK DESCRIPTION: %s
                DETAILED REQUIREMENTS:
                %s
                
                INSTRUCTIONS:
                - Complete the task fully by addressing all requirements listed.
                - Provide the final results in the following structured format:
                  {
                      "longTermMemory": ["..."],
                      "shortTermMemory": ["..."]
                  }
                - Ensure that your response clearly demonstrates completion of the task.
                """,
                title,
                description,
                detailedRequirements.stream().map(req -> "- " + req).collect(Collectors.joining("\n"))
        );
    }

}
