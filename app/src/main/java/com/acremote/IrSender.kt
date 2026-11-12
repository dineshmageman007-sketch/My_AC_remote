package com.acremote

import android.content.Context
import android.hardware.ConsumerIrManager

data class SendResult(val ok: Boolean, val message: String)

object IrSender {
    private fun manager(c: Context): ConsumerIrManager? =
        c.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun hasEmitter(c: Context): Boolean = manager(c)?.hasIrEmitter() == true

    fun send(c: Context, s: AcState, powerChanged: Boolean): SendResult {
        val mgr = manager(c)
        if (mgr == null || !mgr.hasIrEmitter()) {
            return SendResult(false, "No IR blaster found on this phone")
        }
        val ac = SamsungAc()
        ac.setPower(s.power)
        ac.setMode(s.mode)
        ac.setTemp(s.temp)
        ac.setFan(s.fan)
        if (s.mode != SamsungAc.MODE_AUTO) {
            if (s.turbo) ac.setPowerful(true) else if (s.quiet) ac.setQuiet(true)
        }
        ac.setSwing(s.swing)
        val extended = when (s.proto) {
            AcStore.PROTO_21 -> true
            AcStore.PROTO_14 -> false
            else -> powerChanged
        }
        return try {
            mgr.transmit(SamsungAc.CARRIER_HZ, ac.pattern(ac.frame(extended)))
            SendResult(true, "Sent ${if (extended) "21" else "14"}-byte frame")
        } catch (e: Exception) {
            SendResult(false, "Send failed: ${e.message}")
        }
    }
}
