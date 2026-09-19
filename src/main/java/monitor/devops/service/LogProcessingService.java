package monitor.devops.service;

import org.springframework.stereotype.Service;

@Service
public class LogProcessingService {

    public String processLogs(String logs) {

        if (logs == null || logs.isBlank()) {
            return "";
        }

        String[] lines = logs.split("\n");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {

            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            // Remove GitHub timestamp prefix
            int end = line.indexOf("Z ");

            if (end != -1 && end < 30) {
                line = line.substring(end + 2);
            }

            // Keep important lines
            String lower = line.toLowerCase();

            if (lower.contains("error")
                    || lower.contains("exception")
                    || lower.contains("failed")
                    || lower.contains("failure")
                    || lower.contains("warning")
                    || lower.contains("success")
                    || lower.contains("build")
                    || lower.contains("test")
                    || lower.contains("docker")
                    || lower.contains("deploy")) {

                result.append(line).append("\n");
            }

            // Prevent sending huge logs to the LLM
            if (result.length() >= 12000) {
                break;
            }
        }

        return result.toString();
    }
}