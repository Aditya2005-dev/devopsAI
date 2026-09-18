package monitor.devops.controller;

import monitor.devops.service.GitHubService;
import monitor.devops.service.GroqAnalysisService;
import monitor.devops.service.LogProcessingService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GitHubController {

    private final GitHubService githubService;

    private final LogProcessingService logProcessingService;

    private final GroqAnalysisService groqAnalysisService;


    public GitHubController(
            GitHubService githubService,
            LogProcessingService logProcessingService,
            GroqAnalysisService groqAnalysisService) {

        this.githubService = githubService;

        this.logProcessingService =
                logProcessingService;

        this.groqAnalysisService =
                groqAnalysisService;
    }


    // ==============================
    // GitHub User
    // ==============================

    @GetMapping("/api/github/user")
    public Map<String, Object> getUser(
            @AuthenticationPrincipal OAuth2User user) {

        return Map.of(
                "login",
                user.getAttribute("login"),

                "name",
                user.getAttribute("name"),

                "avatarUrl",
                user.getAttribute("avatar_url")
        );
    }


    // ==============================
    // Repositories
    // ==============================

    @GetMapping("/api/github/repositories")
    public String getRepositories(
            @RegisteredOAuth2AuthorizedClient("github")
            OAuth2AuthorizedClient client) {

        return githubService.getRepositories(client);
    }


    // ==============================
    // Workflows
    // ==============================

    @GetMapping(
            "/api/github/workflows/{owner}/{repo}"
    )
    public String getWorkflows(
            @RegisteredOAuth2AuthorizedClient("github")
            OAuth2AuthorizedClient client,

            @PathVariable String owner,

            @PathVariable String repo) {

        return githubService.getWorkflows(
                client,
                owner,
                repo
        );
    }


    // ==============================
    // Workflow Runs
    // ==============================

    @GetMapping(
            "/api/github/workflows/{owner}/{repo}/{workflowId}/runs"
    )
    public String getWorkflowRuns(
            @RegisteredOAuth2AuthorizedClient("github")
            OAuth2AuthorizedClient client,

            @PathVariable String owner,

            @PathVariable String repo,

            @PathVariable long workflowId) {

        return githubService.getWorkflowRuns(
                client,
                owner,
                repo,
                workflowId
        );
    }


    // ==============================
    // Processed Logs
    // ==============================

    @GetMapping(
            "/api/github/runs/{owner}/{repo}/{runId}/logs"
    )
    public String getLogs(
            @RegisteredOAuth2AuthorizedClient("github")
            OAuth2AuthorizedClient client,

            @PathVariable String owner,

            @PathVariable String repo,

            @PathVariable long runId) {


        String rawLogs =
                githubService.getLogs(
                        client,
                        owner,
                        repo,
                        runId
                );


        return logProcessingService.processLogs(
                rawLogs
        );
    }


    // ==============================
    // AI Analysis
    // ==============================

    @PostMapping("/api/github/analyze")
public String analyzeLogs(
        @RequestBody Map<String, String> request) {

    String processedLogs =
            request.get("logs");

    return groqAnalysisService.analyzeLogs(
            processedLogs
    );
}
}