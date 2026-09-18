package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.CatalogRepository
import com.example.data.FirestoreCatalogService
import com.example.data.ProductEntity
import com.example.util.CatalogSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "CatalogSyncWorker"
        const val CHANNEL_ID = "catalog_sync_periodic_channel"
        const val NOTIFICATION_ID = 2001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting fast periodic background catalog sync...")

        try {
            val database = AppDatabase.getDatabase(appContext)
            val repository = CatalogRepository(database.catalogDao())

            CatalogSyncManager.updateProgress(
                isRunning = true,
                message = "Synchronisation rapide des catalogues Firebase..."
            )

            val serverFiles = FirestoreCatalogService.fetchServerCatalogFiles(forceRefresh = true)
            val allProductsToInsert = mutableListOf<ProductEntity>()
            val storesUpdated = mutableSetOf<String>()

            for (file in serverFiles) {
                val prods = if (file.products.isNotEmpty()) {
                    file.products
                } else {
                    FirestoreCatalogService.getDefaultInitialServerFiles()
                        .find { it.catalogType.equals(file.catalogType, ignoreCase = true) }?.products ?: emptyList()
                }
                if (prods.isNotEmpty()) {
                    allProductsToInsert.addAll(prods)
                    storesUpdated.add(file.catalogType)
                }
            }

            if (allProductsToInsert.isNotEmpty()) {
                repository.importProducts(allProductsToInsert)
            }

            val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val summary = "Mise à jour automatique terminée : ${allProductsToInsert.size} produits rafraîchis pour ${storesUpdated.size} enseignes ($timeStr)."

            CatalogSyncManager.updateProgress(
                isRunning = false,
                message = "Synchronisation en arrière-plan réussie",
                current = serverFiles.size,
                total = serverFiles.size,
                summary = summary
            )

            if (allProductsToInsert.isNotEmpty()) {
                showSyncNotification(
                    title = "Catalogues à jour",
                    message = "${allProductsToInsert.size} produits synchronisés automatiquement (${storesUpdated.joinToString(", ")})."
                )
            }

            Log.d(TAG, "Periodic catalog sync finished in fast mode with ${allProductsToInsert.size} items.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Periodic catalog sync failed", e)
            CatalogSyncManager.updateProgress(
                isRunning = false,
                message = "Échec de la synchronisation en arrière-plan",
                isError = true
            )
            Result.retry()
        }
    }

    private fun showSyncNotification(title: String, message: String) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mises à jour automatiques des Catalogues",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications des synchronisations d'arrière-plan"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
