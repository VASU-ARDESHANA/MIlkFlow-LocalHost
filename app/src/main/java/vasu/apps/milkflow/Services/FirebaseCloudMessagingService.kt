package vasu.apps.milkflow.Services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import vasu.apps.milkflow.Activity.MainActivity
import vasu.apps.milkflow.R

class FirebaseCloudMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token : String) {
        super.onNewToken(token)
        Log.d("FirebaseCloudMessagingService", "Refreshed FCM Token: $token")
    }

    override fun onMessageReceived(remoteMessage : RemoteMessage) {
        Log.d("FirebaseCloudMessagingService", "Message received: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title ?: "New Message"
        val message = remoteMessage.notification?.body ?: "You have a new notification"

        showNotification(title, message)
    }

    private fun showNotification(title : String, message : String) {
        val channelId = "default_channel_id"
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.logo_no_bg).setContentTitle(title).setContentText(message).setAutoCancel(true).setSound(defaultSoundUri).setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(0, notificationBuilder.build())
    }
}