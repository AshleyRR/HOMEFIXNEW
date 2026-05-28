package com.tunegocio.homefix.data.model

// Modelo de notificación interna guardada en Firestore
data class NotificationModel(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",       // "nueva solicitud", "tecnico aceptado", "cliente acepto", "completado"
    val requestId: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)