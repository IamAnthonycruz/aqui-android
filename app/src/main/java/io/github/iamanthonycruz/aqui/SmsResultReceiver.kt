package io.github.iamanthonycruz.aqui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * The phone's report for one part of a dwell text, sent by [SmsSender.sendForRow].
 * The row was logged as sent before sending. A failed part turns it into a failed send.
 */
class SmsResultReceiver : BroadcastReceiver() {

    override fun onReceive(c: Context, intent: Intent) {
        val rowId = intent.getLongExtra(EXTRA_ROW_ID, -1)
        if (resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "SMS part sent for row $rowId")
            return
        }
        Log.w(TAG, "SMS part failed for row $rowId, code $resultCode")
        if (rowId < 0) return

        val pending = goAsync()
        Thread { // The database blocks.
            try {
                PingLog.dao(c).markSendFailed(rowId, resultCode)
            } catch (e: Exception) {
                Log.e(TAG, "Could not mark row $rowId failed", e)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val EXTRA_ROW_ID = "row_id"
        private const val TAG = "Aqui"
    }
}
