package monitor.devops.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class LogProcessingService {

    private static final int MAX_OUTPUT_LENGTH = 5000;

    public String processLogs(String logs) {

        if (logs == null || logs.isBlank()) {
            return "";
        }

        String[] lines = logs.split("\\R");

        List<String> importantErrors = new ArrayList<>();
        List<String> importantContext = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Set<String> seen = new LinkedHashSet<>();

        for (int i = 0; i < lines.length; i++) {

            String line = cleanLine(lines[i]);

            if (line.isBlank()) {
                continue;
            }

            String lower = line.toLowerCase();

            /*
             * Highest priority:
             * Actual GitHub Actions errors and failures.
             */
            if (isError(line, lower)) {

                addUnique(importantErrors, seen, line);

                /*
                 * Keep a few lines before an error because the
                 * command/step immediately before the error can
                 * explain what actually failed.
                 */
                for (int j = Math.max(0, i - 2); j < i; j++) {

                    String context = cleanLine(lines[j]);

                    if (!context.isBlank()
                            && !isNoise(context.toLowerCase())) {

                        addUnique(
                                importantContext,
                                seen,
                                context
                        );
                    }
                }

                continue;
            }

            /*
             * Warnings are useful but lower priority than errors.
             */
            if (isWarning(lower)) {

                if (warnings.size() < 8) {
                    addUnique(warnings, seen, line);
                }

                continue;
            }

            /*
             * Keep important CI/CD information such as:
             * failed tests, test summaries, Docker failures,
             * deployment failures, commands and step names.
             */
            if (isImportantContext(lower)) {

                if (importantContext.size() < 35) {
                    addUnique(
                            importantContext,
                            seen,
                            line
                    );
                }
            }
        }

        StringBuilder result = new StringBuilder();

        /*
         * Errors first.
         */
        appendSection(
                result,
                "=== ERRORS / FAILURES ===",
                importantErrors
        );

        /*
         * Then useful surrounding information.
         */
        appendSection(
                result,
                "=== IMPORTANT CONTEXT ===",
                importantContext
        );

        /*
         * Warnings last.
         */
        appendSection(
                result,
                "=== WARNINGS ===",
                warnings
        );

        /*
         * Keep the output short and AI-friendly.
         */
        if (result.length() > MAX_OUTPUT_LENGTH) {

            result.setLength(MAX_OUTPUT_LENGTH);

            result.append(
                    "\n...[remaining logs omitted]..."
            );
        }

        return result.toString().trim();
    }


    private boolean isError(
            String line,
            String lower) {

        return lower.contains("##[error]")
                || lower.contains("error:")
                || lower.contains("error ")
                || lower.contains("exception")
                || lower.contains("failed")
                || lower.contains("failure")
                || lower.contains("build failure")
                || lower.contains("test failure")
                || lower.contains("exit code")
                || lower.contains("process completed with exit code")
                || lower.contains("command failed");
    }


    private boolean isWarning(String lower) {

        return lower.contains("##[warning]")
                || lower.contains("warning:")
                || lower.contains("deprecated")
                || lower.contains("deprecationwarning");
    }


    private boolean isImportantContext(String lower) {

        return lower.contains("job-status")
                || lower.contains("step")
                || lower.contains("run ")
                || lower.contains("running ")
                || lower.contains("starting ")
                || lower.contains("completed ")
                || lower.contains("test")
                || lower.contains("junit")
                || lower.contains("maven")
                || lower.contains("gradle")
                || lower.contains("docker")
                || lower.contains("deploy")
                || lower.contains("deployment")
                || lower.contains("npm")
                || lower.contains("package")
                || lower.contains("build")
                || lower.contains("exit code");
    }


    private boolean isNoise(String lower) {

        return lower.contains("downloading")
                || lower.contains("downloaded")
                || lower.contains("progress")
                || lower.contains("cache")
                || lower.contains("from cache")
                || lower.contains("resolving dependencies")
                || lower.contains("transfer complete");
    }


    private String cleanLine(String line) {

        if (line == null) {
            return "";
        }

        line = line.trim();

        /*
         * Remove GitHub timestamp prefix.
         */
        int timestampEnd = line.indexOf("Z ");

        if (timestampEnd != -1 && timestampEnd < 30) {
            line = line.substring(timestampEnd + 2).trim();
        }

        return line;
    }


    private void addUnique(
            List<String> list,
            Set<String> seen,
            String line) {

        if (line == null || line.isBlank()) {
            return;
        }

        /*
         * Prevent the same log line from appearing multiple times.
         */
        if (seen.add(line)) {
            list.add(line);
        }
    }


    private void appendSection(
            StringBuilder result,
            String title,
            List<String> lines) {

        if (lines.isEmpty()) {
            return;
        }

        if (result.length() > 0) {
            result.append("\n\n");
        }

        result.append(title).append("\n");

        for (String line : lines) {

            /*
             * Stop before exceeding the output limit.
             */
            if (result.length() + line.length() + 1
                    > MAX_OUTPUT_LENGTH) {

                return;
            }

            result.append(line).append("\n");
        }
    }
}