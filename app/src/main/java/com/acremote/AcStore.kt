package com.acremote

import android.content.Context

/** Everything the app remembers about the AC (the AC can't report its state back). */
data class AcState(
    var power: Boolean = false,
    var temp: Int = 24,
    var mode: Int = SamsungAc.MODE_COOL,
    var fan: Int = SamsungAc.FAN_AUTO,
    var swing: Boolean = false,
    var quiet: Boolean = false,
    var turbo: Boolean = false,
    var proto: Int = AcStore.PROTO_14,
    var sleepAt: Long = 0L          // epoch millis when the sleep timer turns the AC off, 0 = none
)

object AcStore {
    const val PROTO_AUTO = 0  // 21-byte only on power changes
    const val PROTO_21 = 1
    const val PROTO_14 = 2    // confirmed working on the AS122USB

    private fun prefs(c: Context) = c.getSharedPreferences("ac", Context.MODE_PRIVATE)

    fun load(c: Context): AcState {
        val p = prefs(c)
        return AcState(
            power = p.getBoolean("power", false),
            temp = p.getInt("temp", 24),
            mode = p.getInt("mode", SamsungAc.MODE_COOL),
            fan = p.getInt("fan", SamsungAc.FAN_AUTO),
            swing = p.getBoolean("swing", false),
            quiet = p.getBoolean("quiet", false),
            turbo = p.getBoolean("turbo", false),
            proto = p.getInt("protocol", PROTO_14),
            sleepAt = p.getLong("sleepAt", 0L)
        )
    }

    fun save(c: Context, s: AcState) {
        prefs(c).edit()
            .putBoolean("power", s.power).putInt("temp", s.temp).putInt("mode", s.mode)
            .putInt("fan", s.fan).putBoolean("swing", s.swing).putBoolean("quiet", s.quiet)
            .putBoolean("turbo", s.turbo).putInt("protocol", s.proto)
            .putLong("sleepAt", s.sleepAt).apply()
    }
}
