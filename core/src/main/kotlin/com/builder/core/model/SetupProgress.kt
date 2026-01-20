package com.builder.core.model

/**
 * Represents the progress of repository setup for Builder deployment.
 */
data class SetupProgress(
    val currentStep: SetupStep,
    val completedSteps: List<SetupStep> = emptyList(),
    val failedStep: SetupStep? = null,
    val errorMessage: String? = null
) {
    val isComplete: Boolean
        get() = currentStep == SetupStep.COMPLETE

    val isFailed: Boolean
        get() = failedStep != null

    val progressPercent: Int
        get() = when (currentStep) {
            SetupStep.CHECKING_EXISTING -> 10
            SetupStep.CREATING_GIST -> 30
            SetupStep.SETTING_VARIABLE -> 50
            SetupStep.SETTING_SECRET -> 70
            SetupStep.CREATING_WORKFLOW -> 90
            SetupStep.COMPLETE -> 100
        }
}

/**
 * Steps in the repository setup process.
 */
enum class SetupStep(val displayName: String, val description: String) {
    CHECKING_EXISTING("Checking Setup", "Checking existing configuration..."),
    CREATING_GIST("Creating Gist", "Creating tunnel status Gist..."),
    SETTING_VARIABLE("Setting Variable", "Setting TUNNEL_GIST_ID variable..."),
    SETTING_SECRET("Setting Secret", "Setting GIST_TOKEN secret..."),
    CREATING_WORKFLOW("Creating Workflow", "Creating builder-deploy.yml..."),
    COMPLETE("Complete", "Setup complete!")
}
