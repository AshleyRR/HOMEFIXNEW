package com.tunegocio.homefix.data

import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.NotificationModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NotificationsRepository {

    private val db = FirebaseFirestore.getInstance()

    // Escucha en tiempo real las notificaciones del usuario ordenaAdas por fecha
    fun obtenerNotificaciones(uid: String): Flow<List<NotificationModel>> = callbackFlow {
        val listener = db.collection("notifications")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val lista = snapshot?.documents?.mapNotNull {
                    it.toObject(NotificationModel::class.java)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }

    // Crea una notificación para un usuario específico
    fun crearNotificacion(
        userId: String,
        titulo: String,
        cuerpo: String,
        tipo: String,
        requestId: String
    ) {
        val id = db.collection("notifications").document().id
        val notificacion = mapOf(
            "id" to id,
            "userId" to userId,
            "title" to titulo,
            "body" to cuerpo,
            "type" to tipo,
            "requestId" to requestId,
            "isRead" to false,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("notifications").document(id).set(notificacion)
    }

    // Escucha la cantidad de notificaciones no leídas
    fun obtenerNoLeidas(uid: String): Flow<Int> = callbackFlow {
        val listener = db.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    // Marca todas las notificaciones del usuario como leídas
    fun marcarTodasComoLeidas(uid: String) {
        db.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit()
            }
    }
}