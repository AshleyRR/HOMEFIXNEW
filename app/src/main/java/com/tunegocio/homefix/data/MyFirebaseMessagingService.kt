package com.tunegocio.homefix.data

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tunegocio.homefix.MainActivity
import com.tunegocio.homefix.R
import com.tunegocio.homefix.data.model.NotificationModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val db = FirebaseFirestore.getInstance()

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val titulo = message.notification?.title ?: message.data["title"] ?: return
        val cuerpo = message.notification?.body ?: message.data["body"] ?: return
        val tipo = message.data["type"] ?: ""
        val requestId = message.data["requestId"] ?: ""

        // Guardar en Firestore
        val notificacion = NotificationModel(
            id = db.collection("notifications").document().id,
            userId = uid,
            title = titulo,
            body = cuerpo,
            type = tipo,
            requestId = requestId,
            isRead = false,
            createdAt = System.currentTimeMillis()
        )
        db.collection("notifications")
            .document(notificacion.id)
            .set(notificacion)

        // Leer preferencias y mostrar notificación local
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = UserPreferences(applicationContext)
            val sonido = prefs.notifSound.first()
            val vibracion = prefs.notifVibration.first()
            mostrarNotificacion(titulo, cuerpo, sonido, vibracion)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).update("fcmToken", token)
    }

    private fun mostrarNotificacion(
        titulo: String,
        cuerpo: String,
        sonido: Boolean,
        vibracion: Boolean
    ) {
        // Intent para abrir la app al tocar la notificación
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))

        // Sonido
        if (sonido) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        } else {
            builder.setSound(null)
        }

        // Vibración
        if (vibracion) {
            builder.setVibrate(longArrayOf(0, 250, 250, 250))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "homefix_notifications"
    }
}