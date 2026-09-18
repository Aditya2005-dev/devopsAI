package monitor.devops.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GroqAnalysisService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String analyzeLogs(String logs) {

        if (logs == null || logs.isBlank())
            return error("No logs available.");

        String prompt = """
                Analyze these GitHub Actions logs.
                Use only the supplied logs.
                Do not invent problems.
                Return ONLY valid JSON.

                {
                  "status":"SUCCESS or FAILURE or WARNING",
                  "summary":"short summary",
                  "rootCause":"root cause or No failure detected",
                  "probableCauses":[
                    {"cause":"name","percentage":70,"reason":"reason"}
                  ],
                  "fixes":["fix"],
                  "warnings":["warning"],
                  "evidence":["log evidence"]
                }

                Percentages must add to 100.

                LOGS:
                """ + logs;

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String,Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are a DevOps log analyzer. Return only JSON."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),
                    "temperature", 0.1,
                    "max_tokens", 800
            );

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            apiUrl,
                            HttpMethod.POST,
                            new HttpEntity<>(body, headers),
                            String.class
                    );

            return extractContent(response.getBody());

        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    private String extractContent(String response) {

    Pattern pattern = Pattern.compile(
        "\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
    );

    Matcher matcher = pattern.matcher(response);

    if (!matcher.find()) {
        return error("AI content not found.");
    }

    String content = matcher.group(1);

    content = content
        .replace("\\\"", "\"")
        .replace("\\\\", "\\");

    content = content
        .replace("```json", "")
        .replace("```", "")
        .trim();

    return content;
}

    private String error(String message) {

        return """
                {
                  "status":"ERROR",
                  "summary":"AI analysis failed.",
                  "rootCause":"Groq API error",
                  "probableCauses":[],
                  "fixes":["Check Groq API configuration."],
                  "warnings":["%s"],
                  "evidence":[]
                }
                """.formatted(message);
    }
}