package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.PriceAlertEntity

object PriceAlertNotificationHelper {
    private const val TAG = "PriceAlertNotification"
    const val CHANNEL_ID = "channel_price_drop_alerts"
    private const val CHANNEL_NAME = "Alertes Baisse de Prix (Price Drops)"
    private const val CHANNEL_DESC = "Notifications d'alerte lorsque le prix d'un produit surveillé passe sous votre seuil défini."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun postPriceDropNotification(
        context: Context,
        alert: PriceAlertEntity,
        newPrice: Double,
        supermarket: String
    ) {
        try {
            createNotificationChannel(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping system notification")
                    return
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAV_DESTINATION", "PRODUCTS")
                putExtra("ALERT_PRODUCT_ID", alert.productId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                alert.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val savings = if (alert.initialPrice > newPrice) alert.initialPrice - newPrice else 0.0
            val savingsText = if (savings > 0) " (Économie: Rs ${String.format("%.2f", savings)})" else ""

            val title = "🚨 Baisse de prix : ${alert.productName}"
            val message = "Nouveau prix : Rs ${String.format("%.2f", newPrice)} chez $supermarket (Seuil : Rs ${String.format("%.2f", alert.targetPrice)})$savingsText"

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_PROMO)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            val notificationId = 10000 + (alert.id.takeIf { it > 0 } ?: alert.productId)
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            Log.d(TAG, "Price drop notification posted for ${alert.productName} at Rs $newPrice")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post price drop notification: ${e.message}", e)
        }
    }
}
