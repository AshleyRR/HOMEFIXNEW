package com.tunegocio.homefix.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.data.model.ReviewModel
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.theme.*
import java.util.UUID

@Composable
fun RatingScreen(
    navController: NavController,
    requestId: String
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var request by remember { mutableStateOf<RequestModel?>(null) }
    var technician by remember { mutableStateOf<UserModel?>(null) }
    var selectedStars by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var starsError by remember { mutableStateOf("") }
    var isAlreadyRated by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        db.collection("requests").document(requestId).get()
            .addOnSuccessListener { doc ->
                request = doc.toObject(RequestModel::class.java)
                request?.technicianId?.let { techId ->
                    if (techId.isNotEmpty()) {
                        db.collection("users").document(techId).get()
                            .addOnSuccessListener { techDoc ->
                                technician = techDoc.toObject(UserModel::class.java)
                            }
                    }
                }
            }

        // Verificar si ya calificó
        db.collection("reviews")
            .whereEqualTo("requestId", requestId)
            .whereEqualTo("clientId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                isAlreadyRated = !snapshot.isEmpty
            }
    }

    fun submitRating() {
        if (selectedStars == 0) {
            starsError = "Selecciona una calificación"
            return
        }
        isLoading = true
        starsError = ""

        val reviewId = UUID.randomUUID().toString()
        val review = ReviewModel(
            reviewId = reviewId,
            requestId = requestId,
            clientId = uid,
            technicianId = request?.technicianId ?: "",
            stars = selectedStars,
            comment = comment.trim(),
            createdAt = System.currentTimeMillis()
        )

        db.collection("reviews").document(reviewId).set(review)
            .addOnSuccessListener {
                // Actualizar promedio del técnico
                val techId = request?.technicianId ?: ""
                if (techId.isNotEmpty()) {
                    db.collection("reviews")
                        .whereEqualTo("technicianId", techId)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val reviews = snapshot.documents.mapNotNull {
                                it.toObject(ReviewModel::class.java)
                            }
                            val average = reviews.map { it.stars }.average().toFloat()
                            db.collection("users").document(techId)
                                .update("rating", average)
                                .addOnSuccessListener {
                                    isLoading = false
                                    navController.navigate(Routes.HOME_CLIENT) {
                                        popUpTo(Routes.HOME_CLIENT) { inclusive = true }
                                    }
                                }
                        }
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            if (isAlreadyRated) {
                // Ya calificó
                Spacer(modifier = Modifier.height(60.dp))
                Text(text = "", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ya calificaste este servicio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                HomefixButton(
                    text = "Volver al inicio",
                    onClick = {
                        navController.navigate(Routes.HOME_CLIENT) {
                            popUpTo(Routes.HOME_CLIENT) { inclusive = true }
                        }
                    }
                )
            } else {
                Text(text = "⭐", fontSize = 52.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Califica el servicio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                technician?.let { tech ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "¿Cómo fue tu experiencia con ${tech.name}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Tarjeta del técnico
                technician?.let { tech ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(50.dp),
                                shape = RoundedCornerShape(25.dp),
                                color = TechnicianColor.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tech.name.firstOrNull()
                                            ?.toString()?.uppercase() ?: "T",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TechnicianColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tech.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = tech.specialties.take(2).joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Selector de estrellas
                Text(
                    text = "Tu calificación",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= selectedStars)
                                Icons.Default.Star
                            else
                                Icons.Default.StarBorder,
                            contentDescription = "Estrella $star",
                            tint = if (star <= selectedStars) Warning else TextHint,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    selectedStars = star
                                    starsError = ""
                                }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Texto descriptivo según estrellas
                if (selectedStars > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (selectedStars) {
                            1 -> "Muy malo 😞"
                            2 -> "Malo 😕"
                            3 -> "Regular 😐"
                            4 -> "Bueno 😊"
                            5 -> "Excelente 🤩"
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = when (selectedStars) {
                            1, 2 -> Error
                            3 -> Warning
                            else -> Success
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (starsError.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = starsError,
                        color = Error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Comentario opcional
                Text(
                    text = "Comentario (opcional)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 300) comment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    placeholder = {
                        Text(
                            "Cuéntanos sobre tu experiencia...",
                            color = TextHint
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = CardBorder
                    )
                )
                Text(
                    text = "${comment.length}/300",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(24.dp))

                HomefixButton(
                    text = "Enviar calificación",
                    onClick = { submitRating() },
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        navController.navigate(Routes.HOME_CLIENT) {
                            popUpTo(Routes.HOME_CLIENT) { inclusive = true }
                        }
                    }
                ) {
                    Text(
                        text = "Omitir por ahora",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}