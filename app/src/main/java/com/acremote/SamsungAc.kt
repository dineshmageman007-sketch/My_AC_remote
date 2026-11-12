package com.acremote

/**
 * Samsung A/C infrared encoder (38 kHz), ported from IRremoteESP8266 ir_Samsung.
 * Verified against the library's reference ON/OFF frames by tools/verify_encoder.py.
 *
 * Frame = 14 bytes (2 sections of 7) for normal changes, or 21 bytes (3 sections)
 * "extended" for power changes. Bytes are sent LSB first.
 */
class SamsungAc {
    companion object {
        const val MIN_TEMP = 16
        const val MAX_TEMP = 30

        const val MODE_AUTO = 0
        const val MODE_COOL = 1
        const val MODE_DRY = 2
        const val MODE_FAN = 3

        const val FAN_AUTO = 0
        const val FAN_LOW = 2
        const val FAN_MED = 4
        const val FAN_HIGH = 5
        const val FAN_AUTO2 = 6   // only valid in Auto mode
        const val FAN_TURBO = 7

        const val CARRIER_HZ = 38000

        private const val HDR_MARK = 690
        private const val HDR_SPACE = 17844
        private const val SECTION_MARK = 3086
        private const val SECTION_SPACE = 8864
        private const val SECTION_GAP = 2886
        private const val BIT_MARK = 586
        private const val ONE_SPACE = 1432
        private const val ZERO_SPACE = 436

        private val RESET = intArrayOf(
            0x02, 0x92, 0x0F, 0x00, 0x00, 0x00, 0xF0,
            0x01, 0x02, 0xAE, 0x71, 0x00, 0x15, 0xF0
        )
        private val MIDDLE = intArrayOf(0x01, 0xD2, 0x0F, 0x00, 0x00, 0x00, 0x00)
    }

    private val raw = RESET.copyOf()

    private fun setBits(idx: Int, shift: Int, width: Int, value: Int) {
        val mask = ((1 shl width) - 1) shl shift
        raw[idx] = (raw[idx] and mask.inv() and 0xFF) or ((value shl shift) and mask)
    }

    private fun getBits(idx: Int, shift: Int, width: Int): Int =
        (raw[idx] shr shift) and ((1 shl width) - 1)

    fun setPower(on: Boolean) {
        val v = if (on) 3 else 0
        setBits(6, 4, 2, v)
        setBits(13, 4, 2, v)
    }

    fun setTemp(t: Int) = setBits(11, 4, 4, t.coerceIn(MIN_TEMP, MAX_TEMP) - MIN_TEMP)

    fun setMode(m: Int) {
        setBits(12, 4, 3, m)
        val fan = getBits(12, 1, 3)
        if (m == MODE_AUTO) setBits(12, 1, 3, FAN_AUTO2)
        else if (fan == FAN_AUTO2) setBits(12, 1, 3, FAN_AUTO)
    }

    fun setFan(f: Int) {
        val inAuto = getBits(12, 4, 3) == MODE_AUTO
        if (f == FAN_AUTO2) { if (inAuto) setBits(12, 1, 3, f); return }
        if (inAuto) return // fan speed not selectable in Auto mode
        setBits(12, 1, 3, f)
    }

    fun setSwing(on: Boolean) = setBits(9, 4, 3, if (on) 0b010 else 0b111)

    fun setQuiet(on: Boolean) {
        setBits(5, 5, 1, if (on) 1 else 0)
        if (on) { setFan(FAN_AUTO); setPowerful(false) }
    }

    fun setPowerful(on: Boolean) {
        setBits(10, 1, 3, if (on) 0b011 else 0b000)
        if (on) { setFan(FAN_TURBO); setQuiet(false) }
    }

    private fun sectionChecksum(b: IntArray, o: Int): Int {
        var n = Integer.bitCount(b[o]) +
            Integer.bitCount(b[o + 1] and 0x0F) +
            Integer.bitCount(b[o + 2] shr 4)
        for (i in 3..6) n += Integer.bitCount(b[o + i])
        return n.inv() and 0xFF
    }

    /** Returns the bytes to transmit: 14 (normal) or 21 (extended, needed for power changes). */
    fun frame(extended: Boolean): IntArray {
        val r = raw.copyOf()
        for (o in intArrayOf(0, 7)) {
            val c = sectionChecksum(r, o)
            r[o + 1] = (r[o + 1] and 0x0F) or ((c and 0xF) shl 4)
            r[o + 2] = (r[o + 2] and 0xF0) or (c shr 4)
        }
        return if (extended) r.copyOfRange(0, 7) + MIDDLE + r.copyOfRange(7, 14) else r
    }

    /** Converts frame bytes to the on/off microsecond pattern for ConsumerIrManager. */
    fun pattern(bytes: IntArray): IntArray {
        val p = ArrayList<Int>(400)
        p.add(HDR_MARK); p.add(HDR_SPACE)
        var o = 0
        while (o < bytes.size) {
            p.add(SECTION_MARK); p.add(SECTION_SPACE)
            for (i in o until o + 7) {
                for (bit in 0 until 8) {
                    p.add(BIT_MARK)
                    p.add(if (((bytes[i] shr bit) and 1) == 1) ONE_SPACE else ZERO_SPACE)
                }
            }
            p.add(BIT_MARK); p.add(SECTION_GAP)
            o += 7
        }
        return p.toIntArray()
    }
}
