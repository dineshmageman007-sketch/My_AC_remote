package com.acremote

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private var st = AcState()

    private lateinit var tempText: TextView
    private lateinit var statusText: TextView
    private lateinit var sleepText: TextView
    private lateinit var protoButton: Button
    private val modeButtons = HashMap<Int, Button>()
    private val fanButtons = HashMap<Int, Button>()
    private lateinit var onButton: Button
    private lateinit var offButton: Button
    private lateinit var turboButton: Button
    private lateinit var quietButton: Button
    private lateinit var swingButton: Button

    private val bg = Color.parseColor("#101418")
    private val card = Color.parseColor("#1B2128")
    private val accent = Color.parseColor("#3DA5FF")
    private val off = Color.parseColor("#2A323B")

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        st = AcStore.load(this)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        // The sleep timer may have fired while the app was closed
        st = AcStore.load(this)
        refresh()
    }

    // ---------- actions ----------

    private fun showResult(r: SendResult) {
        statusText.text = r.message
        statusText.setTextColor(Color.parseColor(if (r.ok) "#8FD19E" else "#FF6B6B"))
    }

    private fun act(v: View, powerChanged: Boolean = false, change: () -> Unit) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        change()
        AcStore.save(this, st)
        refresh()
        showResult(IrSender.send(this, st, powerChanged))
    }

    private fun sleepIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            this, 1, Intent(this, SleepReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun setSleep(minutes: Int) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val show = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), sleepIntent())
        st.sleepAt = at
    }

    private fun clearSleep() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(sleepIntent())
        st.sleepAt = 0L
    }

    // ---------- UI helpers ----------

    private fun styled(b: Button, selected: Boolean) {
        val d = GradientDrawable()
        d.cornerRadius = dp(14).toFloat()
        d.setColor(if (selected) accent else off)
        b.background = d
        b.setTextColor(if (selected) Color.parseColor("#06121F") else Color.WHITE)
    }

    private fun makeButton(label: String, textSp: Float = 15f, onClick: (View) -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.textSize = textSp
        b.typeface = Typeface.DEFAULT_BOLD
        b.minHeight = 0
        b.minimumHeight = 0
        b.setPadding(dp(4), dp(14), dp(4), dp(14))
        b.setOnClickListener { onClick(it) }
        styled(b, false)
        return b
    }

    private fun row(vararg buttons: Button): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        for (b in buttons) {
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(dp(4), dp(4), dp(4), dp(4))
            r.addView(b, lp)
        }
        return r
    }

    private fun label(text: String): TextView {
        val t = TextView(this)
        t.text = text
        t.setTextColor(Color.parseColor("#8A97A6"))
        t.textSize = 13f
        t.setPadding(dp(6), dp(14), 0, dp(2))
        return t
    }

    private fun sleepButton(text: String, minutes: Int): Button =
        makeButton(text, 14f) { v ->
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            setSleep(minutes)
            AcStore.save(this, st)
            refresh()
        }

    // ---------- UI ----------

    private fun buildUi() {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(28), dp(16), dp(28))
        scroll.addView(root)

        val title = TextView(this)
        title.text = "Samsung AC Remote"
        title.setTextColor(Color.WHITE)
        title.textSize = 22f
        title.typeface = Typeface.DEFAULT_BOLD
        root.addView(title)

        statusText = TextView(this)
        statusText.textSize = 13f
        statusText.setPadding(0, dp(4), 0, dp(10))
        val hasIr = IrSender.hasEmitter(this)
        statusText.text = if (hasIr) "IR blaster ready - point the phone's top edge at the AC"
                          else "No IR blaster found on this phone"
        statusText.setTextColor(Color.parseColor(if (hasIr) "#8FD19E" else "#FF6B6B"))
        root.addView(statusText)

        // Temperature card
        val tempCard = LinearLayout(this)
        tempCard.orientation = LinearLayout.VERTICAL
        tempCard.gravity = Gravity.CENTER_HORIZONTAL
        tempCard.setPadding(dp(12), dp(18), dp(12), dp(12))
        val cd = GradientDrawable()
        cd.cornerRadius = dp(20).toFloat()
        cd.setColor(card)
        tempCard.background = cd

        tempText = TextView(this)
        tempText.setTextColor(Color.WHITE)
        tempText.textSize = 64f
        tempText.typeface = Typeface.DEFAULT_BOLD
        tempText.gravity = Gravity.CENTER
        tempCard.addView(tempText)

        val minus = makeButton("-", 28f) { v ->
            act(v) { if (st.temp > SamsungAc.MIN_TEMP) st.temp-- }
        }
        val plus = makeButton("+", 28f) { v ->
            act(v) { if (st.temp < SamsungAc.MAX_TEMP) st.temp++ }
        }
        tempCard.addView(row(minus, plus))
        root.addView(tempCard)

        // Power
        onButton = makeButton("ON", 18f) { v -> act(v, true) { st.power = true } }
        offButton = makeButton("OFF", 18f) { v ->
            act(v, true) {
                st.power = false
                clearSleep() // turning off by hand cancels a pending sleep timer
            }
        }
        root.addView(label("POWER"))
        root.addView(row(onButton, offButton))

        // Mode
        root.addView(label("MODE"))
        val modes = listOf(
            "Auto" to SamsungAc.MODE_AUTO, "Cool" to SamsungAc.MODE_COOL,
            "Dry" to SamsungAc.MODE_DRY, "Fan" to SamsungAc.MODE_FAN
        )
        val modeRow = ArrayList<Button>()
        for ((name, value) in modes) {
            val b = makeButton(name) { v -> act(v) { st.mode = value } }
            modeButtons[value] = b
            modeRow.add(b)
        }
        root.addView(row(*modeRow.toTypedArray()))

        // Fan
        root.addView(label("FAN SPEED"))
        val fans = listOf(
            "Auto" to SamsungAc.FAN_AUTO, "Low" to SamsungAc.FAN_LOW,
            "Med" to SamsungAc.FAN_MED, "High" to SamsungAc.FAN_HIGH
        )
        val fanRow = ArrayList<Button>()
        for ((name, value) in fans) {
            val b = makeButton(name) { v ->
                act(v) { st.fan = value; st.turbo = false; st.quiet = false }
            }
            fanButtons[value] = b
            fanRow.add(b)
        }
        root.addView(row(*fanRow.toTypedArray()))

        // Extras
        root.addView(label("EXTRAS"))
        turboButton = makeButton("Turbo") { v ->
            act(v) { st.turbo = !st.turbo; if (st.turbo) st.quiet = false }
        }
        quietButton = makeButton("Quiet") { v ->
            act(v) { st.quiet = !st.quiet; if (st.quiet) st.turbo = false }
        }
        swingButton = makeButton("Swing") { v -> act(v) { st.swing = !st.swing } }
        root.addView(row(turboButton, quietButton, swingButton))

        // Sleep timer
        root.addView(label("SLEEP TIMER (turns the AC off)"))
        val cancel = makeButton("Cancel", 14f) { v ->
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            clearSleep()
            AcStore.save(this, st)
            refresh()
        }
        root.addView(
            row(
                sleepButton("30 min", 30), sleepButton("1 hr", 60),
                sleepButton("2 hr", 120), sleepButton("4 hr", 240)
            )
        )
        root.addView(row(cancel))
        sleepText = TextView(this)
        sleepText.setTextColor(Color.parseColor("#8A97A6"))
        sleepText.textSize = 13f
        sleepText.setPadding(dp(6), dp(4), dp(6), 0)
        root.addView(sleepText)

        // IR format (troubleshooting)
        root.addView(label("IR FORMAT"))
        protoButton = makeButton("", 13f) { v ->
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            st.proto = (st.proto + 1) % 3
            AcStore.save(this, st)
            refresh()
        }
        root.addView(row(protoButton))

        val hint = TextView(this)
        hint.text = "The sleep timer sends OFF from this phone, so leave the phone pointed at " +
            "the AC until it fires. 14-byte is the format your AC accepts - only change the IR " +
            "format if commands stop working."
        hint.setTextColor(Color.parseColor("#6D7A89"))
        hint.textSize = 12f
        hint.setPadding(dp(6), dp(6), dp(6), 0)
        root.addView(hint)

        setContentView(scroll)
    }

    private fun refresh() {
        tempText.text = "${st.temp}°C"
        styled(onButton, st.power)
        styled(offButton, !st.power)
        for ((k, b) in modeButtons) styled(b, k == st.mode)
        val autoMode = st.mode == SamsungAc.MODE_AUTO
        for ((k, b) in fanButtons) {
            styled(b, !autoMode && !st.turbo && !st.quiet && k == st.fan)
            b.alpha = if (autoMode) 0.4f else 1f
        }
        styled(turboButton, st.turbo && !autoMode)
        styled(quietButton, st.quiet && !autoMode)
        turboButton.alpha = if (autoMode) 0.4f else 1f
        quietButton.alpha = if (autoMode) 0.4f else 1f
        styled(swingButton, st.swing)

        val at = st.sleepAt
        if (at > System.currentTimeMillis()) {
            val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
            sleepText.text = "AC will turn OFF at " + fmt.format(Date(at))
        } else {
            if (at != 0L) {
                st.sleepAt = 0L
                AcStore.save(this, st)
            }
            sleepText.text = "No sleep timer set"
        }

        protoButton.text = when (st.proto) {
            AcStore.PROTO_21 -> "IR format: always 21-byte (tap to change)"
            AcStore.PROTO_AUTO -> "IR format: auto (tap to change)"
            else -> "IR format: 14-byte (tap to change)"
        }
        styled(protoButton, false)
    }
}
