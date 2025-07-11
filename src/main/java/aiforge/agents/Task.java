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
                      "shortTermMemory": ["..."]
                  }
                - There should be an at least 1-to-1 relationship between the detailed requirements and the short-term memory items.
                - Items in short term memory should NOT be a rephrasing of the requirements.
                -- eg if the requirement is "Create a profile of a character named Bob", the short term memory should NOT be "Created a character named Bob".
                -- Instead, it should be something like "Bob is a 35-year-old etc..."
                - Ensure that your response clearly demonstrates completion of the task.
                """,
                title,
                description,
                detailedRequirements.stream().map(req -> "- " + req).collect(Collectors.joining("\n"))
        );
    }

}
