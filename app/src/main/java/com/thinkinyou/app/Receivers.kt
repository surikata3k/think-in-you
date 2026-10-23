package com.thinkinyou.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Recibe las alarmas propias: mostrar una frase o planificar el nuevo día. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Scheduler.ACTION_NOTIFY -> {
                if (!Store(ctx).enabled) return
                Store(ctx).nextPhrase()?.let { Notifier.show(ctx, it) }
            }
            Scheduler.ACTION_PLAN -> Scheduler.ensureScheduled(ctx)
        }
    }
}

/** Las alarmas se borran al reiniciar el móvil; aquí se vuelven a registrar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> Scheduler.ensureScheduled(ctx)
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Scheduler.ensureScheduled(ctx, force = true)
        }
    }
}
