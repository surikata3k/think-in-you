package com.thinkinyou.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga docs/frases.txt del repositorio de GitHub y lo aplica.
 * Si no hay conexión o falla, la app sigue con las frases que ya tiene.
 */
object Remote {

    /** Para usar otro repositorio, cambia esta dirección. */
    const val URL_FRASES =
        "https://raw.githubusercontent.com/surikata3k/think-in-you/main/docs/frases.txt"

    @Volatile private var running = false

    /** Ejecuta la descarga en segundo plano; [done] se llama en el hilo principal. */
    fun sync(ctx: Context, done: (Store.SyncResult?, Boolean) -> Unit) {
        if (running) return
        running = true
        val app = ctx.applicationContext
        Thread {
            var result: Store.SyncResult? = null
            var ok = false
            try {
                val conn = (URL(URL_FRASES).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    useCaches = false
                    setRequestProperty("Cache-Control", "no-cache")
                }
                try {
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                        result = Store(app).syncFrom(body)
                        ok = true
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                // Sin conexión: se intenta de nuevo la próxima vez que se abra la app
            }
            running = false
            Handler(Looper.getMainLooper()).post { done(result, ok) }
        }.start()
    }
}
