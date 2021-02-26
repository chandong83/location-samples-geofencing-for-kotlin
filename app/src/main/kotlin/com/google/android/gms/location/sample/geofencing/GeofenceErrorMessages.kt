package com.google.android.gms.location.sample.geofencing

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofenceStatusCodes

class GeofenceErrorMessages{
    companion object {
        fun getErrorString(context: Context, e: Exception):String {
            return if (e is ApiException) {
                getErrorString(context, e.statusCode)
            } else {
                context.resources.getString(R.string.unknown_geofence_error)
            }
        }

        fun getErrorString(context: Context, errorCode: Int): String {
            val mResources = context.resources
            return when (errorCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> mResources.getString(R.string.geofence_not_available)
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> mResources.getString(R.string.geofence_too_many_geofences)
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> mResources.getString(R.string.geofence_too_many_pending_intents)
                else -> mResources.getString(R.string.unknown_geofence_error)
            }
        }
    }
}
