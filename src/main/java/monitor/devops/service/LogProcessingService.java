package monitor.devops.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class LogProcessingService {

    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T[^ ]+Z\\s*");

    private static final Pattern ANSI_PATTERN =
            Pattern.compile("\\u001B\\[[;\\d]*m");

    // Maximum number of lines sent to Groq
    private static final int MAX_LINES = 80;

    // Lines before and after an important line
    private static final int CONTEXT_LINES = 2;


    public String processLogs(String logs) {

        if (logs == null || logs.isBlank()) {
            return "";
        }

        String[] lines = logs.split("\n");

        Set<Integer> selectedLines = new LinkedHashSet<>();


        for (int i = 0; i < lines.length; i++) {

            String line = cleanLine(lines[i]);

            if (line.isEmpty()) {
                continue;
            }

            if (isImportant(line)) {

                int start =
                        Math.max(0, i - CONTEXT_LINES);

                int end =
                        Math.min(
                                lines.length - 1,
                                i + CONTEXT_LINES
                        );

                for (int j = start; j <= end; j++) {
                    selectedLines.add(j);
                }
            }
        }


        List<String> result = new ArrayList<>();

        for (Integer index : selectedLines) {

            String line = cleanLine(lines[index]);

            if (!line.isEmpty()) {
                result.add(line);
            }

            if (result.size() >= MAX_LINES) {
                break;
            }
        }


        return String.join("\n", result);
    }


    private String cleanLine(String line) {

        line =
                TIMESTAMP_PATTERN
                        .matcher(line)
                        .replaceFirst("");

        line =
                ANSI_PATTERN
                        .matcher(line)
                        .replaceAll("");

        return line.trim();
    }


    private boolean isImportant(String line) {

        String text =
                line.toLowerCase();

        return text.contains("[error]")
                || text.contains("exception")
                || text.contains("warning:")
                || text.contains("tests run:")
                || text.contains("build success")
                || text.contains("build failure")
                || text.contains("process completed")
                || text.contains("exit code")
                || text.contains("compilation")
                || text.contains("docker build")
                || text.contains("login succeeded")
                || text.contains("deployment")
                || text.contains("deploy");
    }
}