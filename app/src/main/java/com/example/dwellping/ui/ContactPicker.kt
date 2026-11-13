package com.example.dwellping.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** One phone number picked from the phone's contacts. */
data class PickedContact(val name: String, val phone: String)

/**
 * The phone's own contact list, showing only entries with a number. Android lets the app read
 * just the one row the user tapped, so no contacts permission is needed. A contact with several
 * numbers shows each one; the user taps the one to text.
 */
private class PickPhone : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) = Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

private fun read(c: Context, uri: Uri): PickedContact? = try {
    c.contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)?.use { cur ->
        if (!cur.moveToFirst()) null
        else PickedContact(name = cur.getString(0).orEmpty(), phone = cur.getString(1).orEmpty())
    }
} catch (e: Exception) {
    null
}

/** Returns a function that opens the picker. [onPicked] runs only when a number was chosen. */
@Composable
fun rememberContactPicker(onPicked: (PickedContact) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(PickPhone()) { uri ->
        val picked = uri?.let { read(context, it) }
        if (picked != null) onPicked(picked)
        else if (uri != null) Toast.makeText(context, "Couldn't read that contact", Toast.LENGTH_SHORT).show()
    }
    return {
        try {
            launcher.launch(Unit)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No contacts app found. Type the number instead.", Toast.LENGTH_LONG).show()
        }
    }
}
