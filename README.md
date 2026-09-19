DevOps Monitor

AI-Assisted CI/CD Failure Investigator for GitHub Actions

DevOps Monitor connects to GitHub, retrieves real GitHub Actions workflow data and execution logs, processes relevant log information, and uses an LLM through Groq to help investigate CI/CD failures.

Project Overview

The application provides a simple dashboard for investigating GitHub Actions workflow runs.

Instead of manually searching through large CI/CD logs, users can select a repository, workflow, and workflow run and receive an AI-assisted analysis containing the probable root cause, supporting evidence, warnings, and suggested fixes.

Features
GitHub OAuth authentication
GitHub repository selection
Workflow selection
Workflow run selection
Real GitHub Actions logs
Log processing and filtering
LLM-based CI/CD log analysis
Probable root cause identification
Suggested fixes
Warnings and supporting evidence
Dockerized Spring Boot application
Render deployment


                         GitHub
                           │
                     GitHub OAuth
                           │
                           ▼
                  ┌─────────────────┐
                  │   Spring Boot   │
                  │     Backend     │
                  └────────┬────────┘
                           │
                    GitHub REST APIs
                           │
                           ▼
                  Workflow Run Logs
                           │
                           ▼
                 Log Processing Service
                           │
                           ▼
                      Groq LLM API
                           │
                           ▼
                   AI Investigation
                           │
                           ▼
                       Dashboard

Tech Stack
Backend
Java
Spring Boot
REST APIs
Spring Security
GitHub OAuth
Frontend
HTML
CSS
JavaScript
LLM
Groq API
openai/gpt-oss-120b
DevOps & Deployment
Docker
GitHub Actions
Render
AI / LLM Integration

The application sends processed GitHub Actions logs to the Groq LLM.

The model analyzes the supplied logs and returns structured information such as:

{
  "status": "FAILURE",
  "summary": "Build failed during dependency resolution.",
  "rootCause": "Dependency resolution failure",
  "probableCauses": [],
  "fixes": [
    "Verify the dependency version and repository configuration."
  ],
  "warnings": [],
  "evidence": []
}

The dashboard displays:

Status
Summary
Root Cause
Probable Causes
Suggested Fixes
Warnings
Evidence

The LLM is instructed to use the supplied logs as evidence and avoid inventing failures.
                       
