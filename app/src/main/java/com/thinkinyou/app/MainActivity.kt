package com.thinkinyou.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {

    private lateinit var store: Store
    private lateinit var root: LinearLayout

    // Paleta (claro / oscuro)
    private val dark by lazy {
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }
    private val cText get() = if (dark) 0xFFEFE7DD.toInt() else 0xFF2B241D.toInt()
    private val cMuted get() = if (dark) 0xFFA79A8C.toInt() else 0xFF7C6F62.toInt()
    private val cCard get() = if (dark) 0xFF27221E.toInt() else 0xFFFFFFFF.toInt()
    private val cQuote get() = if (dark) 0xFF33291F.toInt() else 0xFFF1E4D2.toInt()
    private val cAccent get() = if (dark) 0xFFE0A86E.toInt() else 0xFFB7793F.toInt()
    private val cOnAccent get() = if (dark) 0xFF1C1916.toInt() else 0xFFFFFFFF.toInt()
    private val cLine get() = if (dark) 0xFF3A332D.toInt() else 0xFFEDE4D8.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        Notifier.createChannel(this)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28), dp(18), dp(32))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root, MATCH_PARENT, WRAP_CONTENT)
        }
        setContentView(scroll)
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            @Suppress("DEPRECATION")
            v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        Scheduler.ensureScheduled(this)
        render()
        syncPhrases(manual = false)
    }

    /** Descarga el fichero de frases del repositorio y avisa si ha cambiado algo. */
    private fun syncPhrases(manual: Boolean) {
        Remote.sync(this) { result, ok ->
            if (isFinishing || isDestroyed) return@sync
            when {
                result != null -> {
                    val parts = mutableListOf<String>()
                    if (result.added > 0) parts += "${result.added} nueva(s)"
                    if (result.removed > 0) parts += "${result.removed} retirada(s)"
                    toast("Frases actualizadas" + if (parts.isEmpty()) "" else ": " + parts.joinToString(", "))
                }
                manual && ok -> toast("Las frases ya están al día")
                manual -> toast("No se ha podido conectar. Inténtalo más tarde.")
            }
            render()
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        render()
    }

    // ------------------------------------------------------------------ UI

    private fun render() {
        root.removeAllViews()

        // Cabecera
        root.addView(text("Think in you", 30f, cText, serif = true))
        root.addView(text("Frases para tener presentes", 15f, cMuted).also { it.pad(bottom = 20) })

        renderQuote()
        renderNotifications()
        renderWindows()
        renderLearning()
        renderPhrases()

        root.addView(
            text(
                "Consejo: si los avisos no llegan, en los ajustes del móvil quita a esta app " +
                    "de la optimización de batería.",
                13f, cMuted,
            ).also { it.pad(top = 8) }
        )
    }

    private fun renderQuote() {
        val phrase = store.lastPhrase() ?: store.ensureIntroductions().firstOrNull { it.introduced }
        val card = card(cQuote)
        card.addView(label("FRASE DE AHORA"))
        card.addView(
            text(phrase?.text ?: "Añade alguna frase para empezar.", 21f, cText, serif = true, italic = true)
                .also { it.setLineSpacing(0f, 1.2f); it.pad(top = 8, bottom = 16) }
        )
        val row = row()
        row.addView(pill("Otra frase", primary = true) {
            store.nextPhrase(); render()
        })
        row.addView(space(10))
        row.addView(pill("Probar aviso") {
            val p = store.nextPhrase()
            if (p != null) Notifier.show(this, p)
            if (!notificationsAllowed()) toast("Las notificaciones están desactivadas para esta app")
            render()
        })
        card.addView(row)
        root.addView(card)
    }

    private fun renderNotifications() {
        val card = card()
        val r = row().apply { gravity = Gravity.CENTER_VERTICAL }
        r.addView(text("Avisos activados", 17f, cText).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        r.addView(Switch(this).apply {
            isChecked = store.enabled
            setOnCheckedChangeListener { _, on ->
                store.enabled = on
                Scheduler.ensureScheduled(this@MainActivity, force = true)
                render()
            }
        })
        card.addView(r)

        val status = when {
            !notificationsAllowed() -> "⚠ Android tiene bloqueadas las notificaciones de esta app."
            !store.enabled -> "Pausado. No recibirás frases hasta que lo actives."
            else -> {
                val next = Scheduler.nextTime(this)
                val left = store.plannedTimes.count { it > System.currentTimeMillis() }
                if (next == null) "Por hoy no quedan más avisos. Mañana habrá nuevos."
                else "Quedan $left aviso(s) hoy · el próximo hacia las ${hour(next)}"
            }
        }
        card.addView(text(status, 14f, cMuted).also { it.pad(top = 6) })

        if (!notificationsAllowed()) {
            card.addView(pill("Permitir notificaciones", primary = true) {
                startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                )
            }.also { it.pad(top = 10) })
        }
        root.addView(card)
    }

    private fun renderWindows() {
        val card = card()
        card.addView(label("TRAMOS HORARIOS"))
        card.addView(
            text("Las frases llegan a horas al azar dentro de cada tramo.", 13f, cMuted)
                .also { it.pad(top = 4, bottom = 8) }
        )
        val windows = store.windows
        if (windows.isEmpty()) {
            card.addView(text("No hay tramos. Añade uno para recibir frases.", 15f, cText).also { it.pad(top = 8, bottom = 8) })
        }
        windows.forEachIndexed { i, w ->
            val r = row().apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener { editWindow(i) }
            }
            r.addView(text("${fmt(w.start)} – ${fmt(w.end)}", 18f, cText).apply {
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            })
            r.addView(text("${w.count} ${if (w.count == 1) "aviso" else "avisos"}", 15f, cAccent))
            r.addView(text("  ✕", 18f, cMuted).apply {
                setPadding(dp(12), dp(4), dp(4), dp(4))
                setOnClickListener {
                    confirm("¿Borrar el tramo ${fmt(w.start)} – ${fmt(w.end)}?") {
                        store.windows = windows.filterIndexed { j, _ -> j != i }
                        Scheduler.ensureScheduled(this@MainActivity, force = true)
                        render()
                    }
                }
            })
            if (i > 0) card.addView(divider())
            card.addView(r)
        }
        val total = windows.sumOf { it.count }
        card.addView(text("Total: $total al día", 14f, cMuted).also { it.pad(top = 6, bottom = 10) })
        card.addView(pill("+ Añadir tramo", primary = true) { editWindow(-1) })
        root.addView(card)
    }

    private fun renderLearning() {
        val list = store.phrases()
        val active = list.count { it.introduced }
        val card = card()
        card.addView(label("APRENDIZAJE GRADUAL"))
        card.addView(
            text(
                "Empiezas con pocas frases y cada día entra alguna nueva. " +
                    "Las recién llegadas se repiten más hasta que se asientan.",
                13f, cMuted,
            ).also { it.pad(top = 4, bottom = 10) }
        )
        card.addView(text("En práctica: $active de ${list.size} frases", 17f, cText))

        // Barra de progreso simple
        val track = LinearLayout(this).apply {
            background = rounded(cLine, 4)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(8)).apply { topMargin = dp(8) }
        }
        val frac = if (list.isEmpty()) 0f else active.toFloat() / list.size
        track.addView(View(this).apply {
            background = rounded(cAccent, 4)
            layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, frac)
        })
        track.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f - frac) })
        card.addView(track)

        val r = row().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, dp(6))
            setOnClickListener { editNewPerDay() }
        }
        r.addView(text("Frases nuevas al día", 16f, cText).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        r.addView(text("${store.newPerDay}  ›", 16f, cAccent))
        card.addView(r)

        card.addView(pill("Empezar de nuevo") {
            confirm("Se reinicia el progreso y volverás a empezar con 3 frases.") {
                store.resetProgress()
                store.ensureIntroductions()
                render()
            }
        }.also { it.pad(top = 6) })
        root.addView(card)
    }

    private fun renderPhrases() {
        val card = card()
        card.addView(label("MIS FRASES"))
        val sync = store.lastSync
        val syncText = if (sync == 0L) "Aún no se han sincronizado con GitHub."
        else "Sincronizadas con GitHub: " +
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(sync))
        card.addView(
            text(
                "Se actualizan solas desde el repositorio cada vez que abres la app. " +
                    "Las que añadas aquí entran en práctica desde hoy y no se borran al actualizar.",
                13f, cMuted,
            ).also { it.pad(top = 4, bottom = 4) }
        )
        card.addView(text(syncText, 13f, cMuted).also { it.pad(bottom = 12) })
        val r = row()
        r.addView(pill("+ Añadir frase", primary = true) { addPhrase() })
        r.addView(space(10))
        r.addView(pill("Ver todas (${store.phrases().size})") { listPhrases() })
        card.addView(r)
        card.addView(pill("↻ Actualizar ahora") { syncPhrases(manual = true) }.also { it.pad(top = 10) })
        root.addView(card)
    }

    // ------------------------------------------------------------- Diálogos

    private fun editWindow(index: Int) {
        val current = store.windows.getOrNull(index) ?: TimeWindow(10 * 60, 12 * 60, 1)
        var start = current.start
        var end = current.end

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), dp(4))
        }
        lateinit var startBtn: TextView
        lateinit var endBtn: TextView
        startBtn = pill("Desde  ${fmt(start)}") {
            TimePickerDialog(this, { _, h, m -> start = h * 60 + m; startBtn.text = "Desde  ${fmt(start)}" },
                start / 60, start % 60, true).show()
        }
        endBtn = pill("Hasta  ${fmt(end)}") {
            TimePickerDialog(this, { _, h, m -> end = h * 60 + m; endBtn.text = "Hasta  ${fmt(end)}" },
                end / 60, end % 60, true).show()
        }
        box.addView(startBtn)
        box.addView(endBtn.also { it.pad(top = 10) })
        box.addView(text("Número de avisos en este tramo", 14f, cMuted).also { it.pad(top = 16) })
        val picker = NumberPicker(this).apply {
            minValue = 1; maxValue = 20; value = current.count
        }
        box.addView(picker)

        AlertDialog.Builder(this)
            .setTitle(if (index < 0) "Nuevo tramo" else "Editar tramo")
            .setView(box)
            .setPositiveButton("Guardar") { _, _ ->
                if (end <= start) {
                    toast("La hora de fin debe ser posterior a la de inicio")
                    return@setPositiveButton
                }
                val list = store.windows.toMutableList()
                val w = TimeWindow(start, end, picker.value)
                if (index < 0) list.add(w) else list[index] = w
                store.windows = list
                Scheduler.ensureScheduled(this, force = true)
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editNewPerDay() {
        val picker = NumberPicker(this).apply { minValue = 0; maxValue = 5; value = store.newPerDay }
        AlertDialog.Builder(this)
            .setTitle("Frases nuevas al día")
            .setMessage("0 = repasar solo las que ya tienes en práctica.")
            .setView(picker)
            .setPositiveButton("Guardar") { _, _ -> store.newPerDay = picker.value; render() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addPhrase() {
        val input = EditText(this).apply {
            hint = "Escribe tu frase"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 2
        }
        val box = LinearLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input, MATCH_PARENT, WRAP_CONTENT)
        }
        AlertDialog.Builder(this)
            .setTitle("Nueva frase")
            .setView(box)
            .setPositiveButton("Añadir") { _, _ ->
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) { store.addPhrase(t); toast("Frase añadida"); render() }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun listPhrases() {
        val list = store.phrases().sortedWith(
            compareByDescending<Phrase> { it.introduced }.thenByDescending { it.introducedDay }
        )
        val items = list.map {
            (if (it.introduced) "● " else "○ ") + it.text + if (it.custom) "  (tuya)" else ""
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("● en práctica   ○ pendientes")
            .setItems(items) { _, which ->
                val p = list[which]
                val info = if (p.introduced) "Vista ${p.shown} " + (if (p.shown == 1) "vez" else "veces")
                else "Todavía no ha entrado en práctica"
                AlertDialog.Builder(this)
                    .setTitle(info)
                    .setMessage(p.text)
                    .setPositiveButton("Cerrar", null)
                    .setNegativeButton("Borrar") { _, _ ->
                        confirm("¿Borrar esta frase?") { store.deletePhrase(p.id); render() }
                    }
                    .show()
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun confirm(msg: String, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("Sí") { _, _ -> onYes() }
            .setNegativeButton("No", null)
            .show()
    }

    // -------------------------------------------------------------- Helpers

    private fun notificationsAllowed() =
        getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radiusDp).toFloat()
    }

    private fun card(color: Int = cCard) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(color, 18)
        setPadding(dp(18), dp(16), dp(18), dp(16))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(14) }
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    private fun space(w: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(w), 1) }

    private fun divider() = View(this).apply {
        setBackgroundColor(cLine)
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(1))
    }

    private fun text(
        s: String, sp: Float, color: Int, serif: Boolean = false, italic: Boolean = false,
    ) = TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(color)
        val family = if (serif) Typeface.SERIF else Typeface.DEFAULT
        typeface = Typeface.create(family, if (italic) Typeface.ITALIC else Typeface.NORMAL)
    }

    private fun label(s: String) = text(s, 12f, cAccent).apply {
        letterSpacing = 0.12f
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun pill(s: String, primary: Boolean = false, onClick: () -> Unit) = TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        gravity = Gravity.CENTER
        setPadding(dp(18), dp(11), dp(18), dp(11))
        if (primary) {
            setTextColor(cOnAccent)
            background = rounded(cAccent, 22)
        } else {
            setTextColor(cAccent)
            background = GradientDrawable().apply {
                setStroke(dp(1), cAccent); cornerRadius = dp(22).toFloat()
            }
        }
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun View.pad(top: Int = 0, bottom: Int = 0) {
        val lp = (layoutParams as? LinearLayout.LayoutParams)
            ?: LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        lp.topMargin = dp(top); lp.bottomMargin = dp(bottom)
        layoutParams = lp
    }

    private fun fmt(min: Int) = "%02d:%02d".format(min / 60, min % 60)

    private fun hour(millis: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
