package com.acremote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by the sleep-timer alarm: sends the OFF command (twice, for reliability). */
class SleepReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val st = AcStore.load(context)
        st.power = false
        st.sleepAt = 0L
        AcStore.save(context, st)
        IrSender.send(context, st, true)
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            // ignore
        }
        IrSender.send(context, st, true)
    }
}
