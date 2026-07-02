package com.tunegocio.homefix.data

/*import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tunegocio.homefix.data.model.NotificationModel

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val db = FirebaseFirestore.getInstance()

    // Se llama cuando llega una notificación push con la app abierta
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val titulo = message.notification?.title ?: message.data["title"] ?: return
        val cuerpo = message.notification?.body ?: message.data["body"] ?: return
        val tipo = message.data["type"] ?: ""
        val requestId = message.data["requestId"] ?: ""

        // Guarda la notificación en Firestore para mostrarla en NotificationsScreen
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
    }

    // Se llama cuando FCM genera un nuevo token para el dispositivo
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Guarda el token FCM del dispositivo en Firestore para enviar notificaciones push
        db.collection("users").document(uid)
            .update("fcmToken", token)
    }
}*/