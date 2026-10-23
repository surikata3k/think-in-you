package com.thinkinyou.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

/** Una frase y su progreso de aprendizaje. introducedDay = -1 => aún no ha entrado en práctica. */
data class Phrase(
    val id: Long,
    val text: String,
    var shown: Int = 0,
    var introducedDay: Long = -1,
    var lastShown: Long = 0,
) {
    val introduced get() = introducedDay >= 0
}

/** Tramo horario en minutos desde medianoche (end exclusivo) y nº de avisos en él. */
data class TimeWindow(var start: Int, var end: Int, var count: Int)

/**
 * Guarda todo en SharedPreferences como JSON. Sin bases de datos ni dependencias.
 */
class Store(private val ctx: Context) {

    private val prefs: SharedPreferences =
        ctx.getSharedPreferences("think_in_you", Context.MODE_PRIVATE)

    // ---------- Ajustes ----------

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) = prefs.edit().putBoolean("enabled", v).apply()

    /** Cuántas frases nuevas entran en práctica cada día. */
    var newPerDay: Int
        get() = prefs.getInt("newPerDay", 1)
        set(v) = prefs.edit().putInt("newPerDay", v).apply()

    var windows: List<TimeWindow>
        get() {
            val raw = prefs.getString("windows", null)
                ?: return listOf(TimeWindow(9 * 60, 13 * 60, 2), TimeWindow(17 * 60, 21 * 60, 2))
            val arr = JSONArray(raw)
            return (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                TimeWindow(o.getInt("s"), o.getInt("e"), o.getInt("c"))
            }
        }
        set(list) {
            val arr = JSONArray()
            list.sortedBy { it.start }.forEach {
                arr.put(JSONObject().put("s", it.start).put("e", it.end).put("c", it.count))
            }
            prefs.edit().putString("windows", arr.toString()).apply()
        }

    // ---------- Frases ----------

    fun phrases(): MutableList<Phrase> {
        val raw = prefs.getString("phrases", null) ?: return seedFromAssets()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Phrase(
                o.getLong("id"), o.getString("t"), o.optInt("n"),
                o.optLong("d", -1), o.optLong("l"),
            )
        }.toMutableList()
    }

    fun savePhrases(list: List<Phrase>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("t", it.text).put("n", it.shown)
                    .put("d", it.introducedDay).put("l", it.lastShown)
            )
        }
        prefs.edit().putString("phrases", arr.toString()).apply()
    }

    private fun seedFromAssets(): MutableList<Phrase> {
        val lines = ctx.assets.open("frases.txt").bufferedReader(Charsets.UTF_8).readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
        var id = 1L
        val list = lines.map { Phrase(id++, it) }.toMutableList()
        prefs.edit().putLong("nextId", id).apply()
        savePhrases(list)
        return list
    }

    /** Las frases añadidas por el usuario entran en práctica de inmediato. */
    fun addPhrase(text: String) {
        val list = phrases()
        val id = prefs.getLong("nextId", (list.maxOfOrNull { it.id } ?: 0) + 1)
        prefs.edit().putLong("nextId", id + 1).apply()
        list.add(Phrase(id, text.trim(), introducedDay = today()))
        savePhrases(list)
    }

    fun deletePhrase(id: Long) {
        savePhrases(phrases().filterNot { it.id == id })
    }

    /** Vuelve a empezar: todas las frases sin practicar. */
    fun resetProgress() {
        val list = phrases()
        list.forEach { it.shown = 0; it.introducedDay = -1; it.lastShown = 0 }
        savePhrases(list)
        prefs.edit().remove("lastIntroDay").remove("lastPhraseId").apply()
    }

    // ---------- Aprendizaje gradual ----------

    /**
     * Al empezar entran 3 frases en práctica. Después, cada día nuevo entra
     * [newPerDay] frase(s) más, elegidas al azar entre las pendientes.
     */
    fun ensureIntroductions(list: MutableList<Phrase> = phrases()): MutableList<Phrase> {
        val day = today()
        val pending = list.filter { !it.introduced }.shuffled().toMutableList()
        var changed = false

        if (list.none { it.introduced }) {
            repeat(minOf(3, pending.size)) { pending.removeAt(0).introducedDay = day; changed = true }
            prefs.edit().putLong("lastIntroDay", day).apply()
        } else if (prefs.getLong("lastIntroDay", -1) < day) {
            repeat(minOf(newPerDay, pending.size)) { pending.removeAt(0).introducedDay = day; changed = true }
            prefs.edit().putLong("lastIntroDay", day).apply()
        }
        if (changed) savePhrases(list)
        return list
    }

    /**
     * Elige la siguiente frase. Peso = 1 + 10/(1 + veces vista):
     * una frase nueva (0 vistas) sale ~10 veces más que una muy repasada,
     * así las recientes se repiten más hasta asentarse.
     * Nunca repite la última mostrada si hay alternativa.
     */
    fun nextPhrase(): Phrase? {
        val list = ensureIntroductions()
        val lastId = prefs.getLong("lastPhraseId", -1)
        var pool = list.filter { it.introduced }
        if (pool.size > 1) pool = pool.filter { it.id != lastId }
        if (pool.isEmpty()) return null

        val weights = pool.map { 1.0 + 10.0 / (1 + it.shown) }
        var r = Random.nextDouble(weights.sum())
        var chosen = pool.last()
        for (i in pool.indices) {
            r -= weights[i]
            if (r <= 0) { chosen = pool[i]; break }
        }
        chosen.shown++
        chosen.lastShown = System.currentTimeMillis()
        savePhrases(list)
        prefs.edit().putLong("lastPhraseId", chosen.id).apply()
        return chosen
    }

    fun lastPhrase(): Phrase? {
        val id = prefs.getLong("lastPhraseId", -1)
        return phrases().firstOrNull { it.id == id }
    }

    // ---------- Planificación del día ----------

    var plannedDay: Long
        get() = prefs.getLong("plannedDay", -1)
        set(v) = prefs.edit().putLong("plannedDay", v).apply()

    var plannedTimes: List<Long>
        get() = prefs.getString("plannedTimes", "")!!
            .split(',').filter { it.isNotBlank() }.map { it.toLong() }
        set(v) = prefs.edit().putString("plannedTimes", v.joinToString(",")).apply()

    /** Cuántas alarmas se registraron la última vez (para poder cancelarlas). */
    var scheduledCount: Int
        get() = prefs.getInt("scheduledCount", 0)
        set(v) = prefs.edit().putInt("scheduledCount", v).apply()

    companion object {
        fun today(): Long = LocalDate.now().toEpochDay()
    }
}
