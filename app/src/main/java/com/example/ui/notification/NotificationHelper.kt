package com.example.ui.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.MessageEntity

object NotificationHelper {
    private const val CHANNEL_ID = "new_emails_channel"
    private const val CHANNEL_NAME = "Temporary Inbox Messages"
    private const val CHANNEL_DESC = "Notifications for incoming emails to your temporary address"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNewEmailNotification(
        context: Context,
        message: MessageEntity,
        emailAddress: String,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("MESSAGE_ID", message.id)
            putExtra("ACCOUNT_ID", message.accountId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(message.senderName)
            .setContentText(message.subject)
            .setSubText(emailAddress)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${message.subject}\n\n${message.preview}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!soundEnabled) {
            builder.setSilent(true)
        }

        if (!vibrationEnabled) {
            builder.setVibrate(longArrayOf(0))
        }

        with(NotificationManagerCompat.from(context)) {
            notify(message.id.hashCode(), builder.build())
        }
    }
}
