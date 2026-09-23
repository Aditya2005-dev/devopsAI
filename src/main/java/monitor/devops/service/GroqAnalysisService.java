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

                Return ONLY valid JSON.
                Do not use markdown or code fences.

                IMPORTANT JSON RULES:
                - The response must be valid JSON.
                - Every string must use valid JSON escaping.
                - Never put unescaped double quotes inside a JSON string.
                - Do not use markdown links.
                - Do not return URLs using [text](url) format.
                - Keep evidence short and concise.
                - Do not copy complicated escaping from the logs.
                - If a log contains a long URL, summarize it instead.
                - Do not include raw Docker registry URLs in the response.
                - Do not include backticks in JSON values.

                Return exactly this structure:

                {
                  "status": "SUCCESS or FAILURE or WARNING",
                  "summary": "short summary",
                  "rootCause": "probable root cause or No failure detected",
                  "probableCauses": [
                    {
                      "cause": "cause name",
                      "percentage": 70,
                      "reason": "short reason based on logs"
                    }
                  ],
                  "fixes": [
                    "specific fix"
                  ],
                  "warnings": [
                    "important warning"
                  ],
                  "evidence": [
                    "short important log line"
                  ]
                }

                Rules:
                - Use 1 to 3 probable causes.
                - Percentages must add up to 100.
                - Do not invent causes.
                - If there is no failure, probableCauses can be empty.
                - If successful, rootCause must be "No failure detected".
                - Give short practical fixes.
                - Keep evidence concise.
                - Prefer summarizing long technical lines instead of copying them.
                - If the failure is Docker authentication, describe it as:
                  "Docker registry authentication failed."
                - If the failure is a test failure, identify the failed test if available.
                - If the failure is a deployment failure, identify the deployment problem if available.

                PROCESSED LOGS:
                ----------------
                """ + processedLogs;

        try {

            HttpHeaders headers = new HttpHeaders();

            headers.setBearerAuth(apiKey);
            headers.set("Content-Type", "application/json");

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
                                            """
                                            You are a DevOps CI/CD log analyzer.

                                            Return ONLY valid JSON.
                                            Never return markdown.
                                            Never return code fences.
                                            Never return malformed JSON.
                                            Keep evidence short and readable.
                                            """
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
                            600,

                            "reasoning_effort",
                            "low"
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

            if (responseBody == null ||
                    responseBody.isBlank()) {

                return errorResponse(
                        "Empty response from Groq."
                );
            }

            return extractContent(responseBody);

        } catch (Exception e) {

            return errorResponse(
                    e.getMessage()
            );
        }
    }


    private String extractContent(String response) {

        try {

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

            if (start == -1) {

                return errorResponse(
                        "AI JSON content not found."
                );
            }

            int depth = 0;

            boolean insideString = false;
            boolean escaped = false;

            for (
                    int i = start;
                    i < response.length();
                    i++
            ) {

                char c = response.charAt(i);

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    insideString = !insideString;
                    continue;
                }

                if (!insideString) {

                    if (c == '{') {
                        depth++;
                    }

                    if (c == '}') {

                        depth--;

                        if (depth == 0) {

                            return response
                                    .substring(
                                            start,
                                            i + 1
                                    )
                                    .trim();
                        }
                    }
                }
            }

            return errorResponse(
                    "Incomplete JSON returned by Groq."
            );

        } catch (Exception e) {

            return errorResponse(
                    e.getMessage()
            );
        }
    }


    private String errorResponse(
            String message) {

        /*
         * Prevent an exception message containing quotes,
         * newlines or backslashes from breaking our JSON.
         */
        String safeMessage =
                message == null
                        ? "Unknown error"
                        : message
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\r", " ")
                        .replace("\n", " ");

        return """
                {
                  "status": "ERROR",
                  "summary": "AI analysis failed.",
                  "rootCause": "Groq API error",
                  "probableCauses": [],
                  "fixes": [
                    "Check Groq API configuration or try again later."
                  ],
                  "warnings": [
                    "%s"
                  ],
                  "evidence": []
                }
                """.formatted(
                safeMessage
        );
    }
}