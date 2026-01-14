package com.builder.data.instance

import com.builder.core.model.Instance
import com.builder.core.model.InstanceState
import com.builder.core.model.Pack
import com.builder.core.model.PackType
import com.builder.core.repository.InstanceRepository
import com.builder.data.local.db.dao.InstanceDao
import com.builder.data.local.db.entities.InstanceEntity
import com.builder.runtime.LogCollector
import com.builder.runtime.wasm.WasmRuntime
import com.builder.runtime.workflow.InMemoryKvStore
import com.builder.runtime.workflow.WorkflowContext
import com.builder.runtime.workflow.WorkflowEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File

/**
 * Manages pack instance lifecycle.
 * See Builder_Final.md §12 for instance lifecycle specification.
 *
 * Note: Provided by RuntimeModule, not constructed via @Inject
 */
class InstanceManager(
    private val instanceDao: InstanceDao,
    private val wasmRuntime: WasmRuntime,
    private val httpClient: OkHttpClient,
    private val logCollector: LogCollector
) : InstanceRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val kvStore = InMemoryKvStore()
    private val workflowEngine = WorkflowEngine(wasmRuntime, httpClient, kvStore, logCollector)

    // Track running instances
    private val runningInstances = mutableMapOf<Long, InstanceExecutor>()

    /**
     * Creates a new instance for a pack.
     */
    override suspend fun createInstance(
        pack: Pack,
        name: String
    ): Result<Instance> {
        return try {
            val entity = InstanceEntity(
                packId = pack.id,
                name = name,
                state = InstanceState.STOPPED.name.lowercase(),
                createdAt = System.currentTimeMillis()
            )

            val instanceId = instanceDao.insert(entity)
            val instance = instanceDao.getById(instanceId)?.toDomain()
                ?: throw IllegalStateException("Failed to create instance")

            Timber.i("Instance created: ${instance.id} for pack ${pack.id}")
            Result.success(instance)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create instance")
            Result.failure(e)
        }
    }

    /**
     * Starts an instance.
     */
    override suspend fun startInstance(
        instance: Instance,
        pack: Pack,
        envVars: Map<String, String>
    ): Result<Unit> {
        return try {
            // Check if already running
            if (runningInstances.containsKey(instance.id)) {
                return Result.failure(IllegalStateException("Instance already running"))
            }

            // Update state to running
            val updatedEntity = InstanceEntity.from(
                instance.copy(
                    state = InstanceState.RUNNING,
                    startedAt = System.currentTimeMillis()
                )
            )
            instanceDao.update(updatedEntity)

            // Create executor based on pack type
            val executor = when (pack.type) {
                PackType.WASM -> createWasmExecutor(instance, pack, envVars)
                PackType.WORKFLOW -> createWorkflowExecutor(instance, pack, envVars)
            }

            runningInstances[instance.id] = executor

            // Start execution
            executor.start()

            Timber.i("Instance started: ${instance.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start instance")
            // Revert state
            val revertedEntity = InstanceEntity.from(
                instance.copy(state = InstanceState.STOPPED)
            )
            instanceDao.update(revertedEntity)
            Result.failure(e)
        }
    }

    /**
     * Pauses an instance.
     */
    override suspend fun pauseInstance(instance: Instance): Result<Unit> {
        return try {
            val executor = runningInstances[instance.id]
                ?: return Result.failure(IllegalStateException("Instance not running"))

            executor.pause()

            val updatedEntity = InstanceEntity.from(
                instance.copy(state = InstanceState.PAUSED)
            )
            instanceDao.update(updatedEntity)

            Timber.i("Instance paused: ${instance.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to pause instance")
            Result.failure(e)
        }
    }

    /**
     * Stops an instance.
     */
    override suspend fun stopInstance(instance: Instance): Result<Unit> {
        return try {
            val executor = runningInstances.remove(instance.id)
            executor?.stop()

            val updatedEntity = InstanceEntity.from(
                instance.copy(
                    state = InstanceState.STOPPED,
                    stoppedAt = System.currentTimeMillis()
                )
            )
            instanceDao.update(updatedEntity)

            Timber.i("Instance stopped: ${instance.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop instance")
            Result.failure(e)
        }
    }

    /**
     * Deletes an instance.
     */
    override suspend fun deleteInstance(instanceId: Long): Result<Unit> {
        return try {
            // Stop if running
            runningInstances.remove(instanceId)?.stop()

            instanceDao.deleteById(instanceId)

            Timber.i("Instance deleted: $instanceId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete instance")
            Result.failure(e)
        }
    }

    /**
     * Gets an instance by ID.
     */
    override suspend fun getInstance(instanceId: Long): Instance? {
        return instanceDao.getById(instanceId)?.toDomain()
    }

    /**
     * Gets all instances as Flow.
     */
    override fun getAllInstances(): Flow<List<Instance>> {
        return instanceDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    /**
     * Gets instances for a specific pack.
     */
    override fun getInstancesForPack(packId: String): Flow<List<Instance>> {
        return instanceDao.getByPackId(packId).map { list -> list.map { it.toDomain() } }
    }

    /**
     * Gets running instances.
     */
    override fun getRunningInstances(): Flow<List<Instance>> {
        return instanceDao.getRunning().map { list -> list.map { it.toDomain() } }
    }

    /**
     * Creates a WASM executor.
     */
    private fun createWasmExecutor(
        instance: Instance,
        pack: Pack,
        envVars: Map<String, String>
    ): InstanceExecutor {
        return WasmInstanceExecutor(
            instance = instance,
            pack = pack,
            envVars = envVars,
            wasmRuntime = wasmRuntime
        )
    }

    /**
     * Creates a workflow executor.
     */
    private fun createWorkflowExecutor(
        instance: Instance,
        pack: Pack,
        envVars: Map<String, String>
    ): InstanceExecutor {
        return WorkflowInstanceExecutor(
            instance = instance,
            pack = pack,
            envVars = envVars,
            workflowEngine = workflowEngine,
            packPath = pack.installPath
        )
    }
}

/**
 * Base interface for instance executors.
 */
interface InstanceExecutor {
    fun start()
    fun pause()
    fun stop()
}

/**
 * WASM instance executor.
 */
class WasmInstanceExecutor(
    private val instance: Instance,
    private val pack: Pack,
    private val envVars: Map<String, String>,
    private val wasmRuntime: WasmRuntime
) : InstanceExecutor {

    override fun start() {
        Timber.i("Starting WASM instance: ${instance.id}")
        // TODO: Implement WASM execution
        // This will be completed when Wasmtime native library is integrated
    }

    override fun pause() {
        Timber.i("Pausing WASM instance: ${instance.id}")
        // TODO: Implement pause logic
    }

    override fun stop() {
        Timber.i("Stopping WASM instance: ${instance.id}")
        // TODO: Implement stop logic
    }
}

/**
 * Workflow instance executor.
 */
class WorkflowInstanceExecutor(
    private val instance: Instance,
    private val pack: Pack,
    private val envVars: Map<String, String>,
    private val workflowEngine: WorkflowEngine,
    private val packPath: String
) : InstanceExecutor {

    private val json = Json { ignoreUnknownKeys = true }

    override fun start() {
        Timber.i("Starting workflow instance: ${instance.id}")

        try {
            // Load workflow.json
            val workflowFile = File(packPath, pack.manifest.entry)
            val workflowJson = workflowFile.readText()
            val workflow = json.decodeFromString<com.builder.core.model.Workflow>(workflowJson)

            // Create context
            val context = WorkflowContext(
                packId = pack.id,
                instanceId = instance.id.toString()
            )

            // Execute workflow (async)
            // TODO: Execute in background coroutine and track state
            Timber.i("Workflow loaded: ${workflow.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start workflow")
        }
    }

    override fun pause() {
        Timber.i("Pausing workflow instance: ${instance.id}")
        // Workflows don't support pause in MVP
    }

    override fun stop() {
        Timber.i("Stopping workflow instance: ${instance.id}")
        // TODO: Implement workflow cancellation
    }
}
