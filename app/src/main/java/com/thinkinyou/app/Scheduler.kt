package com.thinkinyou.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

/**
 * Cada día genera horas aleatorias dentro de los tramos configurados
 * y registra una alarma para cada una, más una alarma justo después de
 * medianoche que planifica el día siguiente.
 */
object Scheduler {

    const val ACTION_NOTIFY = "com.thinkinyou.app.NOTIFY"
    const val ACTION_PLAN = "com.thinkinyou.app.PLAN"
    private const val PLAN_REQUEST = 9999

    /**
     * Asegura que hay alarmas registradas.
     * force = true regenera las horas de hoy (p. ej. tras cambiar los tramos).
     */
    fun ensureScheduled(ctx: Context, force: Boolean = false) {
        val store = Store(ctx)
        cancelAll(ctx, store)
        if (!store.enabled) return

        val today = Store.today()
        if (force || store.plannedDay != today) {
            store.plannedTimes = generate(store.windows, LocalDate.now())
            store.plannedDay = today
            store.ensureIntroductions()
        }

        val now = System.currentTimeMillis()
        val times = store.plannedTimes
        val am = ctx.getSystemService(AlarmManager::class.java)
        times.forEachIndexed { i, t ->
            if (t > now) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pending(ctx, ACTION_NOTIFY, i))
            }
        }
        store.scheduledCount = times.size

        // Planificación del día siguiente, a las 00:01
        val tomorrow = LocalDate.now().plusDays(1)
            .atTime(0, 1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tomorrow, pending(ctx, ACTION_PLAN, PLAN_REQUEST))
    }

    /** Horas aleatorias para un día: para cada tramo, `count` minutos distintos al azar. */
    fun generate(windows: List<TimeWindow>, day: LocalDate): List<Long> {
        val zone = ZoneId.systemDefault()
        val result = mutableListOf<Long>()
        for (w in windows) {
            val length = w.end - w.start
            if (length <= 0 || w.count <= 0) continue
            val n = minOf(w.count, length)
            // Reparto en sub-franjas iguales con un punto aleatorio en cada una:
            // sigue siendo impredecible pero evita que dos avisos caigan seguidos.
            val slot = length.toDouble() / n
            for (k in 0 until n) {
                val from = w.start + (k * slot).toInt()
                val to = maxOf(from + 1, w.start + ((k + 1) * slot).toInt())
                val minute = Random.nextInt(from, to)
                val second = Random.nextInt(0, 60)
                val time = LocalTime.of(minute / 60, minute % 60, second)
                result += day.atTime(time).atZone(zone).toInstant().toEpochMilli()
            }
        }
        return result.sorted()
    }

    fun nextTime(ctx: Context): Long? {
        val store = Store(ctx)
        if (!store.enabled || store.plannedDay != Store.today()) return null
        val now = System.currentTimeMillis()
        return store.plannedTimes.firstOrNull { it > now }
    }

    private fun cancelAll(ctx: Context, store: Store) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        // Margen extra por si el número de alarmas cambió
        for (i in 0 until maxOf(store.scheduledCount, 50)) {
            am.cancel(pending(ctx, ACTION_NOTIFY, i))
        }
        am.cancel(pending(ctx, ACTION_PLAN, PLAN_REQUEST))
    }

    private fun pending(ctx: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            ctx, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
