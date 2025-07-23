package com.yourdomain.erdmt

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

object LocationTracker {
    @SuppressLint("MissingPermission")
    fun getLastLocation(context: Context, callback: (Location?) -> Unit) {
        val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { location: Location? ->
            callback(location)
        }
    }
}