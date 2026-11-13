package com.example.dwellping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After a restart or an app update, Android doesn't restart the tracker. Start it again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        if (!Prefs.isEnabled(c) || !Perms.hasAll(c)) return
        DwellService.start(c)
    }
}