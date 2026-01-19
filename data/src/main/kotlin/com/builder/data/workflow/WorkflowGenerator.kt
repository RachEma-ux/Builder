package com.builder.data.workflow

import android.content.Context
import com.builder.core.repository.ProjectType
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates GitHub Actions workflow YAML files for Builder deployment.
 *
 * Workflows are loaded from template files in assets/workflows/ and
 * placeholders are substituted with actual values.
 *
 * Supported placeholders:
 * - {{PACK_NAME}} - The pack name (sanitized repo name)
 * - {{DEFAULT_VERSION}} - Default version string
 */
@Singleton
class WorkflowGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TEMPLATES_PATH = "workflows"

        // Template file names for each project type
        private val TEMPLATE_FILES = mapOf(
            ProjectType.NODEJS to "nodejs.yml",
            ProjectType.PYTHON to "python.yml",
            ProjectType.RUST to "rust.yml",
            ProjectType.GO to "go.yml",
            ProjectType.KOTLIN_JVM to "jvm.yml",
            ProjectType.JAVA to "jvm.yml",
            ProjectType.WASM to "wasm.yml",
            ProjectType.STATIC to "static.yml",
            ProjectType.UNKNOWN to "generic.yml"
        )
    }

    /**
     * Generates a complete builder-deploy.yml workflow for the given project type.
     *
     * @param projectType The detected project type
     * @param packName The sanitized pack name
     * @param defaultVersion Default version to use in workflow inputs
     * @return The generated workflow YAML content
     */
    fun generate(
        projectType: ProjectType,
        packName: String,
        defaultVersion: String = "1.0.0"
    ): String {
        val templateFile = TEMPLATE_FILES[projectType] ?: "generic.yml"

        return try {
            val template = loadTemplate(templateFile)
            substituteplaceholders(template, packName, defaultVersion)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load template $templateFile, using fallback")
            generateFallbackWorkflow(packName, defaultVersion)
        }
    }

    /**
     * Loads a workflow template from assets.
     */
    private fun loadTemplate(fileName: String): String {
        return context.assets.open("$TEMPLATES_PATH/$fileName").bufferedReader().use {
            it.readText()
        }
    }

    /**
     * Substitutes placeholders in the template with actual values.
     */
    private fun substituteplaceholders(
        template: String,
        packName: String,
        defaultVersion: String
    ): String {
        return template
            .replace("{{PACK_NAME}}", packName)
            .replace("{{DEFAULT_VERSION}}", defaultVersion)
    }

    /**
     * Generates a minimal fallback workflow if template loading fails.
     */
    private fun generateFallbackWorkflow(packName: String, defaultVersion: String): String = """
name: Builder Deploy

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version'
        required: true
        default: '$defaultVersion'

permissions:
  contents: write

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Create pack
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          mkdir -p pack-output
          find . -maxdepth 2 -type f \( -name "*.js" -o -name "*.ts" -o -name "*.py" -o -name "*.go" -o -name "*.rs" -o -name "*.wasm" \) -exec cp {} pack-output/ \; 2>/dev/null || true
          echo '{"pack_version":1,"id":"$packName","version":"'${'$'}VERSION'","type":"generic"}' > pack-output/pack.json

      - name: Release
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          cd pack-output && zip -r "../pack-$packName-android-arm64-v${'$'}{VERSION}.zip" .
          cd .. && sha256sum "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" > checksums.sha256
          gh release delete "v${'$'}{VERSION}" --yes 2>/dev/null || true
          gh release create "v${'$'}{VERSION}" "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" checksums.sha256
""".trimIndent()

    /**
     * Lists all available workflow templates.
     */
    fun listAvailableTemplates(): List<String> {
        return try {
            context.assets.list(TEMPLATES_PATH)?.toList() ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to list workflow templates")
            emptyList()
        }
    }

    /**
     * Checks if a template exists for the given project type.
     */
    fun hasTemplate(projectType: ProjectType): Boolean {
        val templateFile = TEMPLATE_FILES[projectType] ?: return false
        return try {
            context.assets.open("$TEMPLATES_PATH/$templateFile").close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
