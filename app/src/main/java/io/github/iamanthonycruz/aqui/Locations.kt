package io.github.iamanthonycruz.aqui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object Locations {
    /** Where you are now, or null if there's no fix or no location permission. */
    @SuppressLint("MissingPermission")
    fun current(c: Context, onResult: (Location?) -> Unit) {
        if (!Perms.state(c).location) { onResult(null); return }
        LocationServices.getFusedLocationProviderClient(c)
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(null) }
    }
}
