package com.example.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.service.CatalogDownloadService
import com.example.service.CatalogSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class WorkSyncStatus(
    val label: String,
    val contentDescription: String,
    val hexColor: Long
) {
    SYNCED("Synced", "Synced", 0xFF10B981),      // Green
    SYNCING("Syncing...", "Syncing...", 0xFFF59E0B), // Yellow / Amber
    OFFLINE("Offline", "Offline", 0xFF9CA3AF),   // Grey
    ERROR("Sync Error", "Sync Error", 0xFFEF4444) // Red
}

data class SyncProgressState(
    val isRunning: Boolean = false,
    val progressMessage: String = "",
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val lastSyncTimeFormatted: String = "",
    val lastSyncTimestamp: Long = 0L,
    val lastSummary: String? = null,
    val isError: Boolean = false
)

object CatalogSyncManager {

    private const val PREFS_NAME = "catalog_sync_prefs"
    private const val KEY_PERIODIC_SYNC_ENABLED = "periodic_work_enabled"
    const val PERIODIC_WORK_TAG = "catalog_periodic_sync_work"
    const val ONE_TIME_WORK_TAG = "catalog_one_time_sync_work"

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _syncState = MutableStateFlow(SyncProgressState())
    val syncState: StateFlow<SyncProgressState> = _syncState.asStateFlow()

    private val _isPeriodicSyncEnabled = MutableStateFlow(true)
    val isPeriodicSyncEnabled: StateFlow<Boolean> = _isPeriodicSyncEnabled.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _workSyncStatus = MutableStateFlow(WorkSyncStatus.SYNCED)
    val workSyncStatus: StateFlow<WorkSyncStatus> = _workSyncStatus.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_PERIODIC_SYNC_ENABLED, true)
        _isPeriodicSyncEnabled.value = enabled

        // Setup Network Connectivity Listener
        setupNetworkObserver(context)

        // Observe WorkManager states
        observeWorkManager(context)

        if (enabled) {
            schedulePeriodicSync(context)
        } else {
            cancelPeriodicSync(context)
        }
    }

    private fun setupNetworkObserver(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                val capabilities = cm.getNetworkCapabilities(activeNetwork)
                val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                _isOnline.value = isConnected
                recomputeSyncStatus()

                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = true
                        recomputeSyncStatus()
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = false
                        recomputeSyncStatus()
                    }

                    override fun onUnavailable() {
                        _isOnline.value = false
                        recomputeSyncStatus()
                    }
                })
            }
        } catch (e: Throwable) {
            android.util.Log.w("CatalogSyncManager", "Network callback setup failed: ${e.message}")
        }
    }

    private fun observeWorkManager(context: Context) {
        try {
            val wm = WorkManager.getInstance(context.applicationContext)
            scope.launch {
                wm.getWorkInfosByTagFlow(PERIODIC_WORK_TAG).collect { workInfos ->
                    updateStatusFromWorkInfos(workInfos)
                }
            }
            scope.launch {
                wm.getWorkInfosByTagFlow(ONE_TIME_WORK_TAG).collect { workInfos ->
                    updateStatusFromWorkInfos(workInfos)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("CatalogSyncManager", "WorkManager observer setup error: ${e.message}")
        }
    }

    private fun updateStatusFromWorkInfos(workInfos: List<WorkInfo>) {
        val anyRunning = workInfos.any { it.state == WorkInfo.State.RUNNING }
        val anyFailed = workInfos.any { it.state == WorkInfo.State.FAILED }
        
        if (anyRunning) {
            _syncState.value = _syncState.value.copy(isRunning = true)
        } else if (anyFailed) {
            _syncState.value = _syncState.value.copy(isRunning = false, isError = true)
        }

        recomputeSyncStatus()
    }

    private fun recomputeSyncStatus() {
        val isRunning = _syncState.value.isRunning
        val isError = _syncState.value.isError
        val online = _isOnline.value

        _workSyncStatus.value = when {
            isRunning -> WorkSyncStatus.SYNCING      // Yellow
            !online -> WorkSyncStatus.OFFLINE        // Grey
            isError -> WorkSyncStatus.ERROR          // Red
            else -> WorkSyncStatus.SYNCED            // Green
        }
    }

    fun setPeriodicSyncEnabled(context: Context, enabled: Boolean) {
        _isPeriodicSyncEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PERIODIC_SYNC_ENABLED, enabled)
            .apply()

        if (enabled) {
            schedulePeriodicSync(context)
        } else {
            cancelPeriodicSync(context)
        }
    }

    fun updateProgress(
        isRunning: Boolean,
        message: String,
        current: Int = 0,
        total: Int = 0,
        summary: String? = null,
        isError: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now))
        _syncState.value = _syncState.value.copy(
            isRunning = isRunning,
            progressMessage = message,
            currentStep = current,
            totalSteps = total,
            lastSummary = summary ?: _syncState.value.lastSummary,
            lastSyncTimestamp = if (!isRunning && !isError && summary != null) now else _syncState.value.lastSyncTimestamp,
            lastSyncTimeFormatted = if (!isRunning && !isError && summary != null) timeStr else _syncState.value.lastSyncTimeFormatted,
            isError = isError
        )
        recomputeSyncStatus()
    }

    /**
     * Enqueues an immediate on-demand sync request using WorkManager
     */
    fun triggerWorkManagerSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<CatalogSyncWorker>()
                .addTag(ONE_TIME_WORK_TAG)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                ONE_TIME_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        } catch (e: Throwable) {
            android.util.Log.w("CatalogSyncManager", "WorkManager trigger immediate failed: ${e.message}")
        }
    }

    /**
     * Schedules periodic background sync using WorkManager (every 6 hours when connected to network)
     */
    fun schedulePeriodicSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<CatalogSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Throwable) {
            android.util.Log.w("CatalogSyncManager", "WorkManager schedule failed: ${e.message}")
        }
    }

    /**
     * Cancels the periodic background sync WorkManager job
     */
    fun cancelPeriodicSync(context: Context) {
        try {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK_TAG)
        } catch (e: Throwable) {
            android.util.Log.w("CatalogSyncManager", "WorkManager cancel failed: ${e.message}")
        }
    }

    /**
     * Starts the Foreground Service to download all Drive catalog files with live persistent notification
     */
    fun startDownloadAllFiles(context: Context) {
        val intent = Intent(context, CatalogDownloadService::class.java).apply {
            action = CatalogDownloadService.ACTION_DOWNLOAD_ALL
        }
        startService(context, intent)
    }

    /**
     * Starts the Foreground Service to download and import a single Drive file
     */
    fun startDownloadSingleFile(context: Context, fileUrl: String, fileName: String, targetCatalog: String) {
        val intent = Intent(context, CatalogDownloadService::class.java).apply {
            action = CatalogDownloadService.ACTION_DOWNLOAD_FILE
            putExtra(CatalogDownloadService.EXTRA_FILE_URL, fileUrl)
            putExtra(CatalogDownloadService.EXTRA_FILE_NAME, fileName)
            putExtra(CatalogDownloadService.EXTRA_TARGET_CATALOG, targetCatalog)
        }
        startService(context, intent)
    }

    /**
     * Cancels any active download or sync task
     */
    fun cancelSync(context: Context) {
        val intent = Intent(context, CatalogDownloadService::class.java).apply {
            action = CatalogDownloadService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    private fun startService(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("CatalogSyncManager", "Failed to start service", e)
        }
    }
}
