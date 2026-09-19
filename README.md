# DevOps Monitor

## AI-Assisted CI/CD Failure Investigator

DevOps Monitor is a web application that helps developers investigate failed GitHub Actions workflow runs.

It connects to GitHub using OAuth, retrieves real GitHub Actions workflow data and logs, processes the logs, and uses an LLM through Groq to identify probable problems, root causes, evidence, warnings, and possible fixes.

---

## Project Overview

Debugging CI/CD failures often requires manually searching through large GitHub Actions logs.

DevOps Monitor simplifies this process by providing a dashboard where users can:

1. Sign in with GitHub
2. Select a repository
3. Select a GitHub Actions workflow
4. Select a workflow run
5. Retrieve the workflow logs
6. Process the logs
7. Analyze the logs using an LLM
8. View the investigation results in a structured dashboard

---

## Features

- GitHub OAuth authentication
- GitHub repository selection
- GitHub Actions workflow selection
- Workflow run selection
- Real GitHub Actions logs
- Log processing and filtering
- AI-assisted CI/CD log analysis
- Probable root cause identification
- Suggested fixes
- Warnings
- Supporting evidence
- Docker containerization
- Render deployment

-Architecture


                         GitHub
                           │
                           │ GitHub OAuth
                           ▼
                  ┌───────────────────┐
                  │    Spring Boot    │
                  │      Backend      │
                  └─────────┬─────────┘
                            │
                            │ GitHub REST API
                            ▼
                  ┌───────────────────┐
                  │ Workflow / Runs   │
                  │      / Logs       │
                  └─────────┬─────────┘
                            │
                            ▼
                  ┌───────────────────┐
                  │ Log Processing    │
                  │     Service       │
                  └─────────┬─────────┘
                            │
                            ▼
                  ┌───────────────────┐
                  │     Groq LLM      │
                  │     Analysis      │
                  └─────────┬─────────┘
                            │
                            ▼
                  ┌───────────────────┐
                  │     Dashboard     │
                  │ Root Cause / Fix  │
                  │ Evidence / Issues │
                  └───────────────────┘
                  

## Tech Stack

### Backend

- Java
- Spring Boot
- Spring Security
- REST APIs
- GitHub OAuth

### Frontend

- HTML
- CSS
- JavaScript

### AI / LLM

- Groq API
- `openai/gpt-oss-120b`

### DevOps & Deployment

- Docker
- GitHub Actions
- Render

---

## AI / LLM Integration

The application sends processed GitHub Actions logs to the Groq LLM.

The model analyzes the supplied logs and returns structured information about the workflow execution.

### Example Response

```json
{
  "status": "FAILURE",
  "summary": "Build failed during dependency resolution.",
  "rootCause": "Dependency resolution failure",
  "probableCauses": [
    {
      "cause": "Invalid dependency configuration",
      "percentage": 70,
      "reason": "The logs indicate that dependency resolution failed."
    }
  ],
  "fixes": [
    "Verify the dependency version and repository configuration."
  ],
  "warnings": [],
  "evidence": [
    "Dependency resolution failed"
  ]
}
