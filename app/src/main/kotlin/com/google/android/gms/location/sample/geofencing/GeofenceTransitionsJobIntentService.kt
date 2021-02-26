package com.google.android.gms.location.sample.geofencing

import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceTransitionsJobIntentService : JobIntentService() {
    companion object  {
        const val TAG = "GeofenceTransitionsKIS";
        const val JOB_ID = 574;
        const val CHANNEL_ID = "channel_02";
        // JobIntentService 시작 함수
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, GeofenceTransitionsJobIntentService::class.java, JOB_ID, intent);
        }
    }

    override fun onHandleWork(intent: Intent) {
        var geofencingEvent: GeofencingEvent = GeofencingEvent.fromIntent(intent)

        //이벤트가 에러인지 확인
        if(geofencingEvent.hasError()) {
            val errorMessage = GeofenceErrorMessages.Companion.getErrorString(this, geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        var geofenceTransition = geofencingEvent.geofenceTransition

        //발생한 이벤트가 입장했거나 나갔다면...
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            val triggeringGeofences = geofencingEvent.triggeringGeofences
            // 알림에 표시할 문자열로 변환
            val geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences)

            // 노티피케이션 알림 출력!
            sendNotification(geofenceTransitionDetails)
            Log.i(TAG, geofenceTransitionDetails)
        } else {
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition))
        }
    }

    // 노티피케이션 알림 출력용 함수
    private fun sendNotification(notificationDetails: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var name = getString(R.string.app_name)
            var channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(applicationContext, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(notificationIntent)

        val notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(resources,
                        R.drawable.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID)
        }
        builder.setAutoCancel(true)

        notificationManager.notify(0, builder.build())
    }

    // 알림으로 표시할 문자열 생성
    // Ex > Exited: 집, 신월사거리, 하이제니스
    private fun getGeofenceTransitionDetails(geofenceTransition: Int, triggeringGeofences: List<Geofence>):String{
        // geofenceTransition 이벤트 타입 ID를 받아 문자열로 변환
        val geofenceTransitionString:String = getTransitionString(geofenceTransition)

        // 이벤트 발생 지역들을 ,을 추가해 문자열로 변환 하기위한 작업
        val triggeringGeofencesIdsList = ArrayList<String>()
        for (geofence in triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.requestId)
        }
        // 집, 신월사거리, 하이제니스
        val triggeringGeofencingIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)

        // 이벤트 발생 : 지역들
        return geofenceTransitionString + ": " + triggeringGeofencingIdsString
    }

    // 이벤트를 문자열로 변환
    fun getTransitionString(transitionType: Int) = when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Entered"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Exited"
            else -> "Unknown Transition"
    }

}
