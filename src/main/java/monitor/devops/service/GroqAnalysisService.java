package monitor.devops.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GroqAnalysisService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();


    public String analyzeLogs(String processedLogs) {

        if (processedLogs == null || processedLogs.isBlank()) {

            return """
                    {
                      "status": "UNKNOWN",
                      "summary": "No processed logs available.",
                      "rootCause": "Not available",
                      "probableCauses": [],
                      "fixes": [],
                      "warnings": [],
                      "evidence": []
                    }
                    """;
        }


        String prompt = """
                Analyze these GitHub Actions processed logs.

                Use ONLY the supplied logs.

                Do not invent problems.

                If the build succeeded, clearly say that
                the build is successful.

                Warnings do not automatically mean failure.

                Return ONLY JSON in this format:

                {
                  "status": "SUCCESS or FAILURE or WARNING",
                  "summary": "short summary",
                  "rootCause": "probable root cause or No failure detected",
                  "probableCauses": [
                    {
                      "cause": "cause name",
                      "percentage": 70,
                      "reason": "reason from logs"
                    }
                  ],
                  "fixes": [
                    "specific fix"
                  ],
                  "warnings": [
                    "important warning"
                  ],
                  "evidence": [
                    "important log line"
                  ]
                }

                Rules:

                - Use 1 to 4 probable causes.
                - Percentages must add up to 100.
                - Percentages are AI estimates based on log evidence.
                - Do not create a cause if the logs do not support it.
                - If there is no failure, probableCauses can be empty.
                - If successful, do not invent a root cause.
                - Give practical fixes based on the logs.

                PROCESSED LOGS:
                ----------------

                """ + processedLogs;


        try {

            HttpHeaders headers = new HttpHeaders();

            headers.setBearerAuth(apiKey);

            headers.set(
                    "Content-Type",
                    "application/json"
            );


            Map<String, Object> requestBody =
                    Map.of(

                            "model",
                            model,

                            "messages",
                            List.of(

                                    Map.of(
                                            "role",
                                            "system",

                                            "content",
                                            "You are a DevOps CI/CD log analyzer. Return only valid JSON."
                                    ),

                                    Map.of(
                                            "role",
                                            "user",

                                            "content",
                                            prompt
                                    )
                            ),

                            "temperature",
                            0.1,

                            "max_tokens",
                            800
                    );


            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(
                            requestBody,
                            headers
                    );


            ResponseEntity<String> response =
                    restTemplate.exchange(
                            apiUrl,
                            HttpMethod.POST,
                            request,
                            String.class
                    );


            String responseBody =
                    response.getBody();


            if (responseBody == null) {

                return errorResponse(
                        "Empty response from Groq."
                );
            }


            /*
             * We return Groq's JSON directly.
             *
             * The frontend will read:
             *
             * status
             * summary
             * rootCause
             * probableCauses
             * fixes
             * warnings
             * evidence
             */

            return extractContent(responseBody);


        } catch (Exception e) {

            return errorResponse(
                    e.getMessage()
            );
        }
    }


    private String extractContent(String response) {

        try {

            /*
             * Groq response looks approximately like:
             *
             * {
             *   "choices": [
             *      {
             *        "message": {
             *           "content": "{...AI JSON...}"
             *        }
             *      }
             *   ]
             * }
             *
             * We don't need complicated Jackson
             * parsing here.
             */

            int contentStart =
                    response.indexOf("\"content\"");


            if (contentStart == -1) {

                return errorResponse(
                        "Groq response did not contain AI content."
                );
            }


            int start =
                    response.indexOf(
                            "{",
                            contentStart
                    );


            int end =
                    response.lastIndexOf("}");


            if (start == -1 || end == -1) {

                return errorResponse(
                        "Invalid JSON returned by Groq."
                );
            }


            String content =
                    response.substring(
                            start,
                            end + 1
                    );


            /*
             * Remove markdown if the model
             * accidentally returns ```json
             */

            content =
                    content
                            .replace("```json", "")
                            .replace("```", "")
                            .trim();


            return content;


        } catch (Exception e) {

            return errorResponse(
                    e.getMessage()
            );
        }
    }


    private String errorResponse(String message) {

        return """
                {
                  "status": "ERROR",
                  "summary": "AI analysis could not be completed.",
                  "rootCause": "Groq API error",
                  "probableCauses": [],
                  "fixes": [
                    "Check the Groq API key and configuration."
                  ],
                  "warnings": [
                    "%s"
                  ],
                  "evidence": []
                }
                """.formatted(
                message == null
                        ? "Unknown error"
                        : message
        );
    }
}