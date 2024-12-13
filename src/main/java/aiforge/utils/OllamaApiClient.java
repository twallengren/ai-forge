package aiforge.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OllamaApiClient {

    private static final String BASE_URL = "http://localhost:11434/api/generate";

    public static OllamaResponse sendRequest(OllamaRequest request) throws IOException {
        // Convert the request object to JSON
        String requestBody = request.toJson();

        // Create the connection
        URL url = new URL(BASE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Send the request body
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(requestBody.getBytes());
            outputStream.flush();
        }

        // Read the response
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(connection.getInputStream(), OllamaResponse.class);
        } else {
            throw new IOException("HTTP error code: " + responseCode);
        }
    }

    public static void main(String[] args) {
        try {
            // Build a sample request
            OllamaRequest.StructuredFormat structuredFormat = new OllamaRequest.StructuredFormat()
                    .addProperty("taskTitle", "string", true)
                    .addProperty("taskPriority", "integer", true)
                    .addArrayProperty("requirements", "string", true);
            OllamaRequest request = new OllamaRequest.Builder()
                    .model(OllamaRequest.Model.LLAMA3P1)
                    .prompt("Generate a task dataset including a title, priority as a number, and a list of requirements.")
                    .structuredFormat(structuredFormat)
                    .stream(false)
                    .build();

            // Send the request and print the response
            OllamaResponse response = sendRequest(request);
            System.out.println("Response: " + response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
