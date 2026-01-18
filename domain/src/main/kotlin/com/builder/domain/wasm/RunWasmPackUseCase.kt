package com.builder.domain.wasm

import com.builder.core.model.ExecutionStatus
import com.builder.core.model.WasmExecutionResult
import com.builder.core.model.WasmExecutionState
import com.builder.core.model.github.WorkflowRun
import com.builder.core.repository.GitHubRepository
import com.builder.core.util.DebugLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for running WASM packs via GitHub Actions.
 *
 * This triggers the CI workflow, polls for completion, and fetches results.
 */
class RunWasmPackUseCase @Inject constructor(
    private val gitHubRepository: GitHubRepository
) {
    companion object {
        private const val POLL_INTERVAL_MS = 5000L // 5 seconds
        private const val MAX_POLL_ATTEMPTS = 60 // 5 minutes max
        private const val WORKFLOW_FILE = "ci.yml"
        private const val WASM_ARTIFACT_NAME = "wasm-execution-results"
    }

    /**
     * Runs a WASM pack via GitHub Actions.
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @param ref Branch or tag to run against
     * @return Flow of execution states
     */
    operator fun invoke(
        owner: String,
        repo: String,
        ref: String = "main"
    ): Flow<WasmExecutionState> = flow {
        DebugLogger.logSync("INFO", "WasmRun", "=== WASM EXECUTION STARTED ===")
        DebugLogger.logSync("INFO", "WasmRun", "Owner: $owner, Repo: $repo, Ref: $ref")
        emit(WasmExecutionState.Triggering)

        // Step 1: Trigger the workflow
        DebugLogger.logSync("INFO", "WasmRun", "Triggering workflow: $WORKFLOW_FILE")
        val triggerResult = gitHubRepository.triggerWorkflow(
            owner = owner,
            repo = repo,
            workflowId = WORKFLOW_FILE,
            ref = ref
        )

        if (triggerResult.isFailure) {
            val errorMsg = triggerResult.exceptionOrNull()?.message ?: "Failed to trigger workflow"
            DebugLogger.logSync("ERROR", "WasmRun", "Trigger failed: $errorMsg")
            emit(WasmExecutionState.Error(errorMsg))
            return@flow
        }
        DebugLogger.logSync("INFO", "WasmRun", "Workflow triggered successfully")

        // Step 2: Wait a bit for the workflow to be registered
        delay(3000)

        // Step 3: Find the workflow run we just triggered
        val runsResult = gitHubRepository.listWorkflowRuns(owner, repo, ref)
        if (runsResult.isFailure) {
            emit(WasmExecutionState.Error(
                runsResult.exceptionOrNull()?.message ?: "Failed to list workflow runs"
            ))
            return@flow
        }

        val runs = runsResult.getOrThrow()
        val latestRun = runs.firstOrNull { it.isRunning() || it.isComplete() }

        if (latestRun == null) {
            DebugLogger.logSync("ERROR", "WasmRun", "Could not find triggered workflow run")
            emit(WasmExecutionState.Error("Could not find the triggered workflow run"))
            return@flow
        }

        DebugLogger.logSync("INFO", "WasmRun", "Workflow run found: ID=${latestRun.id}")
        emit(WasmExecutionState.Running(latestRun.id, "Workflow started..."))

        // Step 4: Poll for completion
        var pollAttempt = 0
        var currentRun: WorkflowRun = latestRun

        while (!currentRun.isComplete() && pollAttempt < MAX_POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            pollAttempt++

            val runResult = gitHubRepository.getWorkflowRun(owner, repo, currentRun.id)
            if (runResult.isSuccess) {
                currentRun = runResult.getOrThrow()
                val status = when (currentRun.status) {
                    "queued" -> "Queued..."
                    "in_progress" -> "Running... (${pollAttempt * 5}s)"
                    else -> currentRun.status
                }
                emit(WasmExecutionState.Running(currentRun.id, status))
            }
        }

        if (!currentRun.isComplete()) {
            DebugLogger.logSync("ERROR", "WasmRun", "Workflow timed out")
            emit(WasmExecutionState.Error("Workflow timed out after ${MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS / 1000} seconds"))
            return@flow
        }

        DebugLogger.logSync("INFO", "WasmRun", "Workflow completed: status=${currentRun.status}, conclusion=${currentRun.conclusion}")

        // Step 5: Fetch execution results from artifacts
        val result = fetchExecutionResult(owner, repo, currentRun.id, currentRun)
        DebugLogger.logSync("INFO", "WasmRun", "=== WASM EXECUTION COMPLETE ===")
        DebugLogger.logSync("INFO", "WasmRun", "Result: ${result.status}, Output: ${result.output.take(100)}")
        emit(WasmExecutionState.Completed(result))
    }

    /**
     * Fetches execution result from workflow artifacts.
     */
    private suspend fun fetchExecutionResult(
        owner: String,
        repo: String,
        runId: Long,
        workflowRun: WorkflowRun
    ): WasmExecutionResult {
        // Get artifacts
        val artifactsResult = gitHubRepository.listArtifacts(owner, repo, runId)

        if (artifactsResult.isFailure) {
            return WasmExecutionResult(
                runId = runId,
                packName = "hello.wasm",
                status = if (workflowRun.isSuccess()) ExecutionStatus.SUCCESS else ExecutionStatus.FAILURE,
                output = "Failed to fetch artifacts: ${artifactsResult.exceptionOrNull()?.message}",
                executedAt = workflowRun.updatedAt,
                duration = null,
                artifactUrl = workflowRun.htmlUrl
            )
        }

        val artifacts = artifactsResult.getOrThrow()
        val wasmArtifact = artifacts.find { it.name == WASM_ARTIFACT_NAME }

        if (wasmArtifact == null) {
            return WasmExecutionResult(
                runId = runId,
                packName = "hello.wasm",
                status = if (workflowRun.isSuccess()) ExecutionStatus.SUCCESS else ExecutionStatus.FAILURE,
                output = "WASM artifact not found. Check workflow run for details.",
                executedAt = workflowRun.updatedAt,
                duration = null,
                artifactUrl = workflowRun.htmlUrl
            )
        }

        // The artifact download URL requires special handling (zip file)
        // For now, we'll return the result with the artifact URL for manual download
        val status = when {
            workflowRun.isSuccess() -> ExecutionStatus.SUCCESS
            workflowRun.isFailed() -> ExecutionStatus.FAILURE
            workflowRun.conclusion == "cancelled" -> ExecutionStatus.CANCELLED
            else -> ExecutionStatus.UNKNOWN
        }

        return WasmExecutionResult(
            runId = runId,
            packName = "hello.wasm",
            status = status,
            output = if (status == ExecutionStatus.SUCCESS) {
                "WASM pack executed successfully!\nDownload artifact for detailed logs."
            } else {
                "Execution failed. Check workflow run for details."
            },
            executedAt = workflowRun.updatedAt,
            duration = null,
            artifactUrl = wasmArtifact.archiveDownloadUrl
        )
    }
}
