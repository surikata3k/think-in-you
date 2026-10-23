package com.thinkinyou.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Notifier {

    private const val CHANNEL = "frases"
    private const val NOTIFICATION_ID = 1

    fun createChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val ch = NotificationChannel(CHANNEL, "Frases", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "Frases para recordar a lo largo del día"
            nm.createNotificationChannel(ch)
        }
    }

    fun show(ctx: Context, phrase: Phrase) {
        createChannel(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return

        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(0xFFB7793F.toInt())
            .setContentTitle("Recuerda")
            .setContentText(phrase.text)
            .setStyle(Notification.BigTextStyle().bigText(phrase.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        // Mismo ID: la frase nueva sustituye a la anterior y no se acumulan.
        nm.notify(NOTIFICATION_ID, n)
    }
}
