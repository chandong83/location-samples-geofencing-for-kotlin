package com.google.android.gms.location.sample.geofencing

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.sample.geofencing.Constants.Companion.BAY_AREA_LANDMARKS
import com.google.android.gms.location.sample.geofencing.GeofenceErrorMessages.Companion.getErrorString
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MainActivity : AppCompatActivity(), OnCompleteListener<Void> {
    private val TAG = MainActivity::class.java.simpleName
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    private enum class PendingGeofenceTask {
        ADD, REMOVE, NONE
    }
    lateinit var mGeofencingClient: GeofencingClient
    lateinit var mGeofenceList: ArrayList<Geofence>
    private var mGeofencePendingIntent: PendingIntent? = null

    private var mAddGeofencesButton: Button? = null
    private var mRemoveGeofencesButton: Button? = null

    private var mPendingGeofenceTask = PendingGeofenceTask.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        mAddGeofencesButton = findViewById<View>(R.id.add_geofences_button) as Button
        mRemoveGeofencesButton = findViewById<View>(R.id.remove_geofences_button) as Button

        mGeofenceList = ArrayList()

        mGeofencePendingIntent = null

        setButtonsEnabledState()
        populateGeofenceList()

        mGeofencingClient = LocationServices.getGeofencingClient(this)
    }

    override fun onStart() {
        super.onStart()

        if(!checkPermissions()) {
            requestPermissions()
        } else {
            performPendingGeofenceTask()
        }
    }


    private fun getGeofencingRequest(): GeofencingRequest {
        val builder = GeofencingRequest.Builder()
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
        builder.addGeofences(mGeofenceList)
        return builder.build()
    }

    fun addGeofencesButtonHandler(view: View?) {
        if (!checkPermissions()) {
            mPendingGeofenceTask = PendingGeofenceTask.ADD
            requestPermissions()
            return
        }
        addGeofences()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofences() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions))
            return
        }
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnCompleteListener(this)
    }

    fun removeGeofencesButtonHandler(view: View?) {
        if (!checkPermissions()) {
            mPendingGeofenceTask = PendingGeofenceTask.REMOVE
            requestPermissions()
            return
        }
        removeGeofences()
    }

    @SuppressLint("MissingPermission")
    private fun removeGeofences() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions))
            return
        }
        mGeofencingClient.removeGeofences(getGeofencePendingIntent())?.addOnCompleteListener(this)
    }

    override fun onComplete(task: Task<Void>) {
    //override fun onComplete(@NonNull Task<Void> task) {
        mPendingGeofenceTask = PendingGeofenceTask.NONE
        if(task.isSuccessful) {
            updateGeofencesAdded(!getGeofencesAdded())
            setButtonsEnabledState()
            val messageId = if (getGeofencesAdded()) R.string.geofences_added else R.string.geofences_removed
            Toast.makeText(this, getString(messageId), Toast.LENGTH_SHORT).show()
        } else {
            Log.e(TAG, "Error Code " + (task.exception as ApiException).statusCode)
            val errorMessage: String = getErrorString(this, task.exception as ApiException)
            Log.w(TAG, errorMessage)
        }
    }

    private fun getGeofencePendingIntent(): PendingIntent? {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent
        }
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        mGeofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return mGeofencePendingIntent
    }

    private fun populateGeofenceList() {
        for ((key, value) in BAY_AREA_LANDMARKS) {
            Log.i(TAG, "name " + key + "lat " + value.latitude + ", lon" + value.longitude)
            mGeofenceList.add(Geofence.Builder()
                    .setRequestId(key)
                    .setCircularRegion(
                            value.latitude,
                            value.longitude,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build())
        }
    }

    private fun setButtonsEnabledState() {
        if (getGeofencesAdded()) {
            mAddGeofencesButton!!.isEnabled = false
            mRemoveGeofencesButton!!.isEnabled = true
        } else {
            mAddGeofencesButton!!.isEnabled = true
            mRemoveGeofencesButton!!.isEnabled = false
        }
    }
    private fun showSnackbar(text: String) {
        val container: View = findViewById<ViewGroup>(android.R.id.content)
        Snackbar.make(container, text, Snackbar.LENGTH_LONG).show()
    }

    private fun showSnackbar(mainTextStringId: Int, actionStringId: Int,
                     listener: View.OnClickListener) {
        Snackbar.make(
                findViewById<ViewGroup>(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show()
    }

    private fun getGeofencesAdded(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                Constants.GEOFENCES_ADDED_KEY, false)
    }
    private fun updateGeofencesAdded(added: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Constants.GEOFENCES_ADDED_KEY, added)
                .apply()
    }
    private fun performPendingGeofenceTask() {
        if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
            addGeofences()
        } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
            removeGeofences()
        }
    }
    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        val hasPermissionAccessBackgroundLocation:Boolean = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermissionAccessBackgroundLocation) {
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackbar(R.string.permission_rationale, android.R.string.ok
            ) { // Request permission
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE)
            }
        } else {
            Log.i(TAG, "Requesting permission")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>,
                                   grantResults: IntArray) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted.")
                performPendingGeofenceTask()
            } else {
                showSnackbar(R.string.permission_denied_explanation, R.string.settings
                ) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package",
                            BuildConfig.APPLICATION_ID, null)
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
                mPendingGeofenceTask = PendingGeofenceTask.NONE
            }
        }
    }
}