package monitor.devops.service;

import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GitHubService {

    private final RestTemplate restTemplate = new RestTemplate();


    public String getRepositories(OAuth2AuthorizedClient client) {

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(
                client.getAccessToken().getTokenValue()
        );

        return restTemplate.exchange(
                "https://api.github.com/user/repos",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        ).getBody();
    }


    public String getWorkflows(
            OAuth2AuthorizedClient client,
            String owner,
            String repo) {

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(
                client.getAccessToken().getTokenValue()
        );

        String url =
                "https://api.github.com/repos/"
                + owner + "/"
                + repo
                + "/actions/workflows";

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        ).getBody();
    }


    public String getWorkflowRuns(
            OAuth2AuthorizedClient client,
            String owner,
            String repo,
            long workflowId) {

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(
                client.getAccessToken().getTokenValue()
        );

        String url =
                "https://api.github.com/repos/"
                + owner + "/"
                + repo
                + "/actions/workflows/"
                + workflowId
                + "/runs";

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        ).getBody();
    }


    public String getLogs(
            OAuth2AuthorizedClient client,
            String owner,
            String repo,
            long runId) {

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(
                client.getAccessToken().getTokenValue()
        );

        String url =
                "https://api.github.com/repos/"
                + owner + "/"
                + repo
                + "/actions/runs/"
                + runId
                + "/logs";

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        byte[].class
                );

        return extractLogs(response.getBody());
    }


    private String extractLogs(byte[] zipData) {

        StringBuilder logs = new StringBuilder();

        try {

            ZipInputStream zip =
                    new ZipInputStream(
                            new ByteArrayInputStream(zipData)
                    );

            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {

                if (!entry.isDirectory()) {

                    ByteArrayOutputStream output =
                            new ByteArrayOutputStream();

                    byte[] buffer = new byte[1024];

                    int length;

                    while ((length = zip.read(buffer)) != -1) {
                        output.write(buffer, 0, length);
                    }

                    logs.append(
                            output.toString()
                    );

                    logs.append("\n\n");
                }
            }

            zip.close();

        } catch (Exception e) {

            return "Failed to extract GitHub Actions logs: "
                    + e.getMessage();
        }

        return logs.toString();
    }
}