let currentProcessedLogs = "";

let currentRun = null;


// ============================================
// PAGE LOAD
// ============================================

document.addEventListener(
    "DOMContentLoaded",
    function () {

        loadUser();

        loadRepositories();

        setupEvents();

    }
);


// ============================================
// EVENTS
// ============================================

function setupEvents() {

    document
        .getElementById("repository")
        .addEventListener(
            "change",
            changeRepository
        );


    document
        .getElementById("workflow")
        .addEventListener(
            "change",
            changeWorkflow
        );


    document
        .getElementById("run")
        .addEventListener(
            "change",
            changeRun
        );


    document
        .getElementById("analyzeButton")
        .addEventListener(
            "click",
            analyzeLogs
        );
}


// ============================================
// USER
// ============================================

function loadUser() {

    fetch("/api/github/user")

        .then(response => {

            if (!response.ok) {
                throw new Error(
                    "Unable to load GitHub user"
                );
            }

            return response.json();

        })

        .then(user => {

            document.getElementById(
                "username"
            ).textContent =
                user.name || user.login;

        })

        .catch(error => {

            console.error(error);

        });
}


// ============================================
// REPOSITORIES
// ============================================

function loadRepositories() {

    fetch("/api/github/repositories")

        .then(response => response.json())

        .then(data => {

            const repository =
                document.getElementById(
                    "repository"
                );


            repository.innerHTML =
                '<option value="">Select repository</option>';


            data.forEach(repo => {

                const option =
                    document.createElement(
                        "option"
                    );

                option.value =
                    repo.owner.login +
                    "/" +
                    repo.name;

                option.textContent =
                    repo.name;


                repository.appendChild(
                    option
                );

            });

        })

        .catch(error => {

            console.error(error);

        });
}


// ============================================
// REPOSITORY SELECTED
// ============================================

function changeRepository() {

    const value =
        document.getElementById(
            "repository"
        ).value;


    const workflow =
        document.getElementById(
            "workflow"
        );


    const run =
        document.getElementById(
            "run"
        );


    workflow.innerHTML =
        '<option value="">Select workflow</option>';


    run.innerHTML =
        '<option value="">Select run</option>';


    workflow.disabled = true;

    run.disabled = true;


    resetAnalysis();

    clearLogs();


    if (!value) {
        return;
    }


    const parts =
        value.split("/");


    const owner = parts[0];

    const repo = parts[1];


    loadWorkflows(
        owner,
        repo
    );
}


// ============================================
// WORKFLOWS
// ============================================

function loadWorkflows(
    owner,
    repo
) {

    fetch(
        `/api/github/workflows/${owner}/${repo}`
    )

        .then(response => response.json())

        .then(data => {

            const workflow =
                document.getElementById(
                    "workflow"
                );


            workflow.innerHTML =
                '<option value="">Select workflow</option>';


            data.workflows.forEach(item => {

                const option =
                    document.createElement(
                        "option"
                    );

                option.value =
                    item.id;

                option.textContent =
                    item.name;


                workflow.appendChild(
                    option
                );

            });


            workflow.disabled = false;

        })

        .catch(error => {

            console.error(error);

        });
}


// ============================================
// WORKFLOW SELECTED
// ============================================

function changeWorkflow() {

    const workflowId =
        document.getElementById(
            "workflow"
        ).value;


    const repository =
        document.getElementById(
            "repository"
        ).value;


    const run =
        document.getElementById(
            "run"
        );


    run.innerHTML =
        '<option value="">Select run</option>';

    run.disabled = true;


    resetAnalysis();

    clearLogs();


    if (!workflowId || !repository) {
        return;
    }


    const parts =
        repository.split("/");


    loadRuns(
        parts[0],
        parts[1],
        workflowId
    );
}


// ============================================
// WORKFLOW RUNS
// ============================================

function loadRuns(
    owner,
    repo,
    workflowId
) {

    fetch(
        `/api/github/workflows/${owner}/${repo}/${workflowId}/runs`
    )

        .then(response => response.json())

        .then(data => {

            const run =
                document.getElementById(
                    "run"
                );


            run.innerHTML =
                '<option value="">Select run</option>';


            data.workflow_runs.forEach(item => {

                const option =
                    document.createElement(
                        "option"
                    );


                option.value =
                    item.id;


                const conclusion =
                    item.conclusion ||
                    item.status;


                option.textContent =
                    "#" +
                    item.run_number +
                    " — " +
                    conclusion +
                    " — " +
                    formatDate(
                        item.created_at
                    );


                option.dataset.run =
                    JSON.stringify(item);


                run.appendChild(
                    option
                );

            });


            run.disabled = false;

        })

        .catch(error => {

            console.error(error);

        });
}


// ============================================
// RUN SELECTED
// ============================================

function changeRun() {

    const select =
        document.getElementById(
            "run"
        );


    const option =
        select.options[
            select.selectedIndex
        ];


    if (!option || !option.value) {

        currentRun = null;

        resetAnalysis();

        clearLogs();

        return;
    }


    const repository =
        document.getElementById(
            "repository"
        ).value;


    const parts =
        repository.split("/");


    const runData =
        JSON.parse(
            option.dataset.run
        );


    currentRun = {

        owner: parts[0],

        repo: parts[1],

        runId: option.value,

        data: runData

    };


    displayBuildStatus(
        runData
    );


    loadLogs(
        currentRun.owner,
        currentRun.repo,
        currentRun.runId
    );
}


// ============================================
// BUILD STATUS
// ============================================

function displayBuildStatus(run) {

    const status =
        document.getElementById(
            "buildStatus"
        );


    const conclusion =
        run.conclusion ||
        run.status ||
        "unknown";


    status.textContent =
        conclusion.toUpperCase();


    status.className =
        "build-status " +
        getStatusClass(
            conclusion
        );


    document.getElementById(
        "buildTitle"
    ).textContent =
        run.name ||
        "GitHub Actions Run";


    document.getElementById(
        "buildMessage"
    ).textContent =
        getBuildMessage(
            conclusion
        );


    document.getElementById(
        "runStatus"
    ).textContent =
        run.status || "—";


    document.getElementById(
        "runConclusion"
    ).textContent =
        run.conclusion || "—";


    document.getElementById(
        "runStarted"
    ).textContent =
        formatDate(
            run.created_at
        );


    document.getElementById(
        "runCompleted"
    ).textContent =
        formatDate(
            run.updated_at
        );


    document.getElementById(
        "runDuration"
    ).textContent =
        calculateDuration(
            run.created_at,
            run.updated_at
        );
}


function getStatusClass(status) {

    status =
        String(status)
            .toLowerCase();


    if (status === "success") {

        return "success";

    }


    if (
        status === "failure" ||
        status === "failed"
    ) {

        return "failure";

    }


    if (
        status === "cancelled" ||
        status === "timed_out"
    ) {

        return "failure";

    }


    if (
        status === "in_progress" ||
        status === "queued"
    ) {

        return "running";

    }


    return "neutral";
}


function getBuildMessage(conclusion) {

    conclusion =
        String(conclusion)
            .toLowerCase();


    if (conclusion === "success") {

        return "The GitHub Actions run completed successfully.";

    }


    if (
        conclusion === "failure" ||
        conclusion === "failed"
    ) {

        return "The GitHub Actions run failed. Review the processed logs and AI investigation below.";

    }


    if (conclusion === "cancelled") {

        return "The GitHub Actions run was cancelled.";

    }


    if (conclusion === "timed_out") {

        return "The GitHub Actions run timed out.";

    }


    return "The selected GitHub Actions run is not completed.";
}


// ============================================
// LOGS
// ============================================

function loadLogs(
    owner,
    repo,
    runId
) {

    const logs =
        document.getElementById(
            "logs"
        );


    const logCount =
        document.getElementById(
            "logCount"
        );


    const analyzeButton =
        document.getElementById(
            "analyzeButton"
        );


    logs.textContent =
        "Loading processed logs...";


    logCount.textContent =
        "Loading...";


    analyzeButton.disabled =
        true;


    currentProcessedLogs = "";


    fetch(
        `/api/github/runs/${owner}/${repo}/${runId}/logs`
    )

        .then(response => {

            if (!response.ok) {

                throw new Error(
                    "Unable to load logs"
                );

            }

            return response.text();

        })

        .then(data => {

            currentProcessedLogs =
                data;


            logs.textContent =
                data ||
                "No processed logs available.";


            const lines =
                data
                    .split("\n")
                    .filter(
                        line =>
                            line.trim()
                                .length > 0
                    );


            logCount.textContent =
                lines.length +
                " lines";


            analyzeButton.disabled =
                !data.trim();

        })

        .catch(error => {

            logs.textContent =
                "Unable to load processed logs.";


            logCount.textContent =
                "Error";


            console.error(error);

        });
}


// ============================================
// AI ANALYSIS
// ============================================

function analyzeLogs() {

    if (!currentProcessedLogs) {

        return;

    }


    const button =
        document.getElementById(
            "analyzeButton"
        );


    button.disabled = true;

    button.textContent =
        "Analyzing...";


    resetAnalysisText(
        "Analyzing processed logs..."
    );


    fetch(
        "/api/github/analyze",
        {

            method: "POST",

            headers: {
                "Content-Type":
                    "application/json"
            },

            body: JSON.stringify({

                logs:
                    currentProcessedLogs

            })

        }
    )

        .then(response => {

            if (!response.ok) {

                return response
                    .text()
                    .then(message => {

                        throw new Error(
                            message
                        );

                    });

            }

            return response.json();

        })

        .then(data => {

            displayAnalysis(
                data
            );

        })

        .catch(error => {

            console.error(error);


            document.getElementById(
                "aiSummary"
            ).textContent =
                "AI analysis could not be completed.";


            document.getElementById(
                "rootCause"
            ).textContent =
                "Unable to analyze logs.";

        })

        .finally(() => {

            button.disabled = false;

            button.textContent =
                "Analyze with AI";

        });
}


// ============================================
// DISPLAY AI RESULT
// ============================================

function displayAnalysis(data) {

    document.getElementById(
        "aiSummary"
    ).textContent =
        data.summary ||
        "No summary returned.";


    document.getElementById(
        "rootCause"
    ).textContent =
        data.rootCause ||
        "No root cause identified.";


    displayProbableCauses(
        data.probableCauses
    );


    displayList(
        "fixes",
        data.fixes,
        "No fixes suggested."
    );


    displayList(
        "warnings",
        data.warnings,
        "No important warnings."
    );


    displayList(
        "evidence",
        data.evidence,
        "No evidence returned."
    );
}


// ============================================
// PROBABLE CAUSES
// ============================================

function displayProbableCauses(
    causes
) {

    const container =
        document.getElementById(
            "probableCauses"
        );


    container.innerHTML = "";


    if (
        !Array.isArray(causes) ||
        causes.length === 0
    ) {

        container.innerHTML =
            '<div class="empty-analysis">No failure causes identified.</div>';

        return;
    }


    causes.forEach(item => {

        const row =
            document.createElement(
                "div"
            );


        row.className =
            "cause-row";


        const top =
            document.createElement(
                "div"
            );


        top.className =
            "cause-top";


        const name =
            document.createElement(
                "strong"
            );


        name.textContent =
            item.cause ||
            "Unknown cause";


        const percentage =
            document.createElement(
                "span"
            );


        percentage.textContent =
            (item.percentage || 0) +
            "%";


        top.appendChild(name);

        top.appendChild(
            percentage
        );


        const bar =
            document.createElement(
                "div"
            );


        bar.className =
            "cause-bar";


        const fill =
            document.createElement(
                "div"
            );


        fill.className =
            "cause-fill";


        fill.style.width =
            Math.min(
                Number(
                    item.percentage
                ) || 0,
                100
            ) + "%";


        bar.appendChild(
            fill
        );


        const reason =
            document.createElement(
                "p"
            );


        reason.textContent =
            item.reason ||
            "";


        row.appendChild(top);

        row.appendChild(bar);

        row.appendChild(reason);


        container.appendChild(
            row
        );

    });
}


// ============================================
// LIST DISPLAY
// ============================================

function displayList(
    elementId,
    items,
    emptyText
) {

    const container =
        document.getElementById(
            elementId
        );


    container.innerHTML = "";


    if (
        !Array.isArray(items) ||
        items.length === 0
    ) {

        container.innerHTML =
            `<div class="empty-analysis">${emptyText}</div>`;

        return;
    }


    items.forEach(item => {

        const div =
            document.createElement(
                "div"
            );


        div.className =
            "list-item";


        div.textContent =
            typeof item === "string"
                ? item
                : JSON.stringify(item);


        container.appendChild(
            div
        );

    });
}


// ============================================
// RESET
// ============================================

function resetAnalysis() {

    document.getElementById(
        "aiSummary"
    ).textContent =
        "Select a workflow run and analyze its processed logs.";


    document.getElementById(
        "rootCause"
    ).textContent =
        "—";


    document.getElementById(
        "probableCauses"
    ).innerHTML =
        '<div class="empty-analysis">No analysis available.</div>';


    document.getElementById(
        "fixes"
    ).innerHTML =
        '<div class="empty-analysis">No fixes available.</div>';


    document.getElementById(
        "warnings"
    ).innerHTML =
        '<div class="empty-analysis">No warnings available.</div>';


    document.getElementById(
        "evidence"
    ).innerHTML =
        '<div class="empty-analysis">No evidence available.</div>';
}


function resetAnalysisText(text) {

    document.getElementById(
        "aiSummary"
    ).textContent =
        text;


    document.getElementById(
        "rootCause"
    ).textContent =
        "Analyzing...";


    document.getElementById(
        "probableCauses"
    ).innerHTML =
        '<div class="empty-analysis">Analyzing...</div>';


    document.getElementById(
        "fixes"
    ).innerHTML =
        '<div class="empty-analysis">Analyzing...</div>';

}


function clearLogs() {

    currentProcessedLogs = "";


    document.getElementById(
        "logs"
    ).textContent =
        "Select a workflow run to load processed logs.";


    document.getElementById(
        "logCount"
    ).textContent =
        "0 lines";


    document.getElementById(
        "analyzeButton"
    ).disabled =
        true;
}


// ============================================
// DATE / DURATION
// ============================================

function formatDate(date) {

    if (!date) {
        return "—";
    }


    const value =
        new Date(date);


    if (isNaN(value)) {
        return "—";
    }


    return value.toLocaleString();
}


function calculateDuration(
    start,
    end
) {

    if (!start || !end) {

        return "—";

    }


    const startTime =
        new Date(start);


    const endTime =
        new Date(end);


    const seconds =
        Math.floor(
            (endTime - startTime) / 1000
        );


    if (seconds < 0) {

        return "—";

    }


    const minutes =
        Math.floor(
            seconds / 60
        );


    const remainingSeconds =
        seconds % 60;


    if (minutes === 0) {

        return remainingSeconds + "s";

    }


    return (
        minutes +
        "m " +
        remainingSeconds +
        "s"
    );
}