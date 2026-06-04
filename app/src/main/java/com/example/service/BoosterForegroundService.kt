package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class BoosterForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "ag_booster_active_channel"
        private const val NOTIFICATION_ID = 4821

        fun startService(context: Context) {
            val intent = Intent(context, BoosterForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Fallback direct start to keep robust on all systems
                try {
                    context.startService(intent)
                } catch (ex: Exception) {}
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BoosterForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification(
            "AG Booster Active Engine", 
            "Optimisasi CPU & Anti-Lag Jaringan 100% Berjalan Stabil"
        )
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Safe fallback
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(title: String, text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AG Booster Active Optimization Engine",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Saluran prioritas untuk mengunci RAM, menjaga CPU, dan mengkoreksi latency jaringan."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
