package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.CatalogRepository
import com.example.util.CatalogSyncManager
import com.example.util.SpreadsheetImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CatalogDownloadService : Service() {

    companion object {
        const val TAG = "CatalogDownloadService"
        const val CHANNEL_ID = "catalog_download_channel"
        const val NOTIFICATION_ID = 1001
        const val COMPLETE_NOTIFICATION_ID = 1002

        const val ACTION_DOWNLOAD_ALL = "com.example.service.ACTION_DOWNLOAD_ALL"
        const val ACTION_DOWNLOAD_FILE = "com.example.service.ACTION_DOWNLOAD_FILE"
        const val ACTION_CANCEL = "com.example.service.ACTION_CANCEL"

        const val EXTRA_FILE_URL = "EXTRA_FILE_URL"
        const val EXTRA_FILE_NAME = "EXTRA_FILE_NAME"
        const val EXTRA_TARGET_CATALOG = "EXTRA_TARGET_CATALOG"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var currentDownloadJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    private val defaultDriveFiles = listOf(
        DriveFileSpec("Dreamprice_Catalogue_Global.xlsx", "DREAMPRICE", "https://drive.google.com/uc?export=download&id=dreamprice_file_001"),
        DriveFileSpec("Intermart_Prix_Et_Promotions.csv", "INTERMART", "https://drive.google.com/uc?export=download&id=intermart_file_002"),
        DriveFileSpec("SuperU_Brochure_Alimentation.xlsx", "SUPER U", "https://drive.google.com/uc?export=download&id=superu_file_003"),
        DriveFileSpec("Winners_Catalogue_Hebdo.csv", "WINNERS", "https://drive.google.com/uc?export=download&id=winners_file_004"),
        DriveFileSpec("Jumbo_Promotions_Et_Surgeles.xlsx", "JUMBO", "https://drive.google.com/uc?export=download&id=jumbo_file_005"),
        DriveFileSpec("Carrefour_Catalogue_Import.csv", "CARREFOUR", "https://drive.google.com/uc?export=download&id=carrefour_file_006"),
        DriveFileSpec("KingSavers_Epicerie_Et_Grains.xlsx", "KING SAVERS", "https://drive.google.com/uc?export=download&id=kingsavers_file_007")
    )

    private data class DriveFileSpec(val name: String, val targetCatalog: String, val url: String)

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == null) {
            try {
                startForegroundWithNotification("Service de synchronisation")
            } catch (e: Exception) {
                Log.w(TAG, "startForeground error", e)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_DOWNLOAD_ALL -> {
                startForegroundWithNotification("Démarrage du téléchargement global...")
                handleDownloadAll()
            }
            ACTION_DOWNLOAD_FILE -> {
                val url = intent.getStringExtra(EXTRA_FILE_URL) ?: ""
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Catalogue"
                val catalog = intent.getStringExtra(EXTRA_TARGET_CATALOG) ?: "DREAMPRICE"
                startForegroundWithNotification("Téléchargement de $fileName...")
                handleDownloadSingleFile(url, fileName, catalog)
            }
            ACTION_CANCEL -> {
                cancelCurrentOperation()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Téléchargements & Synchronisation Catalogue",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progression du téléchargement et parsing des catalogues en arrière-plan"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification(initialText: String) {
        val notification = buildProgressNotification(initialText, 0, 100, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildProgressNotification(
        contentText: String,
        progress: Int,
        max: Int,
        indeterminate: Boolean
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, CatalogDownloadService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kwic-Kart — Importation en arrière-plan")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setProgress(max, progress, indeterminate)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Annuler", cancelPendingIntent)
            .build()
    }

    private fun updateProgress(text: String, current: Int, total: Int) {
        val indeterminate = total <= 0
        val notification = buildProgressNotification(text, current, total.coerceAtLeast(1), indeterminate)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun handleDownloadAll() {
        currentDownloadJob?.cancel()
        currentDownloadJob = serviceScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = CatalogRepository(database.catalogDao())
            var totalProducts = 0
            val importedStores = mutableMapOf<String, Int>()

            CatalogSyncManager.updateProgress(
                isRunning = true,
                message = "Synchronisation rapide des catalogues...",
                current = 0,
                total = 100
            )

            try {
                updateProgress("Récupération des catalogues serveur...", 20, 100)
                val serverFiles = com.example.data.FirestoreCatalogService.fetchServerCatalogFiles(forceRefresh = true)
                val allProductsToInsert = mutableListOf<com.example.data.ProductEntity>()
                val totalFiles = serverFiles.size.coerceAtLeast(1)

                serverFiles.forEachIndexed { index, file ->
                    val fileStep = index + 1
                    val progressPercent = 20 + ((fileStep.toFloat() / totalFiles) * 60).toInt()
                    val msg = "($fileStep/$totalFiles) Synchronisation : ${file.catalogType} (${file.fileName})"
                    updateProgress(msg, progressPercent, 100)
                    CatalogSyncManager.updateProgress(
                        isRunning = true,
                        message = msg,
                        current = fileStep,
                        total = totalFiles
                    )

                    val prods = if (file.products.isNotEmpty()) {
                        file.products
                    } else {
                        com.example.data.FirestoreCatalogService.getDefaultInitialServerFiles()
                            .find { it.catalogType.equals(file.catalogType, ignoreCase = true) }?.products ?: emptyList()
                    }

                    if (prods.isNotEmpty()) {
                        allProductsToInsert.addAll(prods)
                        importedStores[file.catalogType] = (importedStores[file.catalogType] ?: 0) + prods.size
                    }
                }

                if (allProductsToInsert.isNotEmpty()) {
                    updateProgress("Enregistrement local dans la base Room...", 90, 100)
                    repository.importProducts(allProductsToInsert)
                    totalProducts = allProductsToInsert.size
                }

                updateProgress("Finalisation de la synchronisation...", 100, 100)
                val breakdown = importedStores.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                val summary = "Synchronisation ultra-rapide réussie : $totalProducts produits dans ${importedStores.size} enseignes ($breakdown)"

                CatalogSyncManager.updateProgress(
                    isRunning = false,
                    message = "Synchronisation globale terminée avec succès",
                    current = totalFiles,
                    total = totalFiles,
                    summary = summary
                )

                showCompletionNotification("Synchronisation terminée", summary)
            } catch (e: Exception) {
                Log.e(TAG, "Download all failed", e)
                val errorMsg = "Erreur : ${e.localizedMessage ?: "Échec de connexion"}"
                CatalogSyncManager.updateProgress(
                    isRunning = false,
                    message = errorMsg,
                    isError = true
                )
                showCompletionNotification("Erreur de synchronisation", errorMsg)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun handleDownloadSingleFile(url: String, fileName: String, targetCatalog: String) {
        currentDownloadJob?.cancel()
        currentDownloadJob = serviceScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = CatalogRepository(database.catalogDao())

            CatalogSyncManager.updateProgress(
                isRunning = true,
                message = "Téléchargement de $fileName...",
                current = 0,
                total = 1
            )

            try {
                updateProgress("Téléchargement et analyse de $fileName...", 0, 1)
                val result = SpreadsheetImporter.downloadAndParse(url) { progressMsg ->
                    updateProgress(progressMsg, 50, 100)
                }

                repository.importProducts(result.products)
                val summary = "${result.products.size} produits importés dans ${result.targetCatalog} depuis $fileName"

                CatalogSyncManager.updateProgress(
                    isRunning = false,
                    message = "Fichier $fileName importé avec succès",
                    current = 1,
                    total = 1,
                    summary = summary
                )

                showCompletionNotification("Importation terminée", summary)
            } catch (e: Exception) {
                Log.e(TAG, "Single file download failed", e)
                val errorMsg = "Erreur : ${e.localizedMessage ?: "Échec du téléchargement"}"
                CatalogSyncManager.updateProgress(
                    isRunning = false,
                    message = errorMsg,
                    isError = true
                )
                showCompletionNotification("Erreur d'importation", errorMsg)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun cancelCurrentOperation() {
        currentDownloadJob?.cancel()
        CatalogSyncManager.updateProgress(
            isRunning = false,
            message = "Téléchargement annulé par l'utilisateur",
            isError = false
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun showCompletionNotification(title: String, message: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(COMPLETE_NOTIFICATION_ID, notification)
    }
}
