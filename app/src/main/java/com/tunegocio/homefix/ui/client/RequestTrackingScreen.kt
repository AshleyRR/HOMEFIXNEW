package com.tunegocio.homefix.ui.client

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

// NUEVO - MULTIDIOMA:
// Permite obtener textos y plurales desde strings_client.xml.
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.NotificationsRepository
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.data.model.ReviewModel
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.theme.*

// NUEVO - MULTIDIOMA:
// Permite acceder a las claves del módulo cliente.
import com.tunegocio.homefix.R
import java.util.UUID


import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.foundation.BorderStroke

// NUEVO - MULTIDIOMA:
// Traduce únicamente la etiqueta visible del servicio o especialidad.
// El valor interno permanece sin cambios para conservar la compatibilidad
// con Firebase, filtros y solicitudes existentes.
@Composable
private fun trackingServiceLabel(serviceType: String): String = when (serviceType) {
    "Electricidad" -> stringResource(R.string.service_electricity)
    "Gasfitería" -> stringResource(R.string.service_plumbing)
    "Pintura" -> stringResource(R.string.service_painting)
    "Carpintería" -> stringResource(R.string.service_carpentry)
    "Vidriería" -> stringResource(R.string.service_glasswork)
    "Jardinería" -> stringResource(R.string.service_gardening)
    "Cerrajería" -> stringResource(R.string.service_locksmith)
    "Albañilería" -> stringResource(R.string.service_masonry)
    "Muebles a medida" -> stringResource(R.string.service_custom_furniture)
    "Lavado de tapizados" -> stringResource(R.string.service_upholstery_cleaning)
    "Mudanzas" -> stringResource(R.string.service_moving)
    else -> serviceType
}

// NUEVO - MULTIDIOMA:
// Genera una lista visible de especialidades traducidas sin modificar
// los valores originales almacenados en el perfil del técnico.
@Composable
private fun trackingSpecialtiesLabel(specialties: List<String>): String {
    val labels = mapOf(
        "Electricidad" to stringResource(R.string.service_electricity),
        "Gasfitería" to stringResource(R.string.service_plumbing),
        "Pintura" to stringResource(R.string.service_painting),
        "Carpintería" to stringResource(R.string.service_carpentry),
        "Vidriería" to stringResource(R.string.service_glasswork),
        "Jardinería" to stringResource(R.string.service_gardening),
        "Cerrajería" to stringResource(R.string.service_locksmith),
        "Albañilería" to stringResource(R.string.service_masonry),
        "Muebles a medida" to stringResource(R.string.service_custom_furniture),
        "Lavado de tapizados" to stringResource(R.string.service_upholstery_cleaning),
        "Mudanzas" to stringResource(R.string.service_moving)
    )

    return specialties.joinToString(", ") { specialty ->
        labels[specialty] ?: specialty
    }
}

@Composable
fun RequestTrackingScreen(
    navController: NavController,
    requestId: String
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val uid = auth.currentUser?.uid ?: ""

    var request by remember { mutableStateOf<RequestModel?>(null) }
    var tecnicosInteresados by remember { mutableStateOf<List<UserModel>>(emptyList()) }
    var eligiendoTecnicoId by remember { mutableStateOf("") }
    val notificationsRepo = remember { NotificationsRepository() }
    var technician by remember { mutableStateOf<UserModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var trabajosRealizadosMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var reviewExistente by remember { mutableStateOf<ReviewModel?>(null) }
    var fotoExpandidaUrl by remember { mutableStateOf<String?>(null) }

    var clienteName by remember { mutableStateOf("") }

    // Confirmación completado
    var showConfirmarCompletadoDialog by remember { mutableStateOf(false) }
    var showRechazarCompletadoDialog by remember { mutableStateOf(false) }

    //  Confirmación sin continuar
    //var showConfirmarSinContinuarDialog by remember { mutableStateOf(false) }

    // Modal de calificación inline
    var showRatingModal by remember { mutableStateOf(false) }
    var selectedStars by remember { mutableStateOf(0) }
    var ratingComment by remember { mutableStateOf("") }
    var ratingLoading by remember { mutableStateOf(false) }
    var starsError by remember { mutableStateOf("") }

    // NUEVO - MULTIDIOMA:
    // Estos textos se resuelven durante la composición para poder usarlos
    // después dentro de callbacks de Firebase y funciones locales.
    val currentServiceLabel =
        trackingServiceLabel(request?.serviceType ?: "")

    val defaultClientName =
        stringResource(R.string.tracking_default_client_name)

    val cancelNotificationTitle =
        stringResource(R.string.tracking_notification_request_canceled_title)
    val cancelNotificationBody =
        stringResource(
            R.string.tracking_notification_request_canceled_body,
            currentServiceLabel
        )
    val cancelNotificationNamedBody =
        stringResource(
            R.string.tracking_notification_request_canceled_named_body,
            clienteName.ifEmpty { defaultClientName },
            currentServiceLabel
        )

    val chosenNotificationTitle =
        stringResource(R.string.tracking_notification_chosen_title)
    val chosenNotificationBody =
        stringResource(
            R.string.tracking_notification_chosen_body,
            currentServiceLabel
        )
    val otherChosenNotificationTitle =
        stringResource(R.string.tracking_notification_other_chosen_title)
    val otherChosenNotificationBody =
        stringResource(R.string.tracking_notification_other_chosen_body)

    val notConfirmedNotificationTitle =
        stringResource(R.string.tracking_notification_not_confirmed_title)
    val notConfirmedNotificationBody =
        stringResource(R.string.tracking_notification_not_confirmed_body)

    val notContinuedNotificationTitle =
        stringResource(R.string.tracking_notification_not_continued_title)
    val notContinuedNotificationBody =
        stringResource(R.string.tracking_notification_not_continued_body)

    val ratedNotificationTitle =
        stringResource(R.string.tracking_notification_rated_title)
    val ratedNotificationBody =
        pluralStringResource(
            R.plurals.tracking_stars_received,
            selectedStars.coerceAtLeast(1),
            selectedStars,
            currentServiceLabel
        )

    val whatsappMessage =
        stringResource(R.string.tracking_client_whatsapp_message)

    val selectRatingError =
        stringResource(R.string.tracking_select_rating_error)

    // Escucha en tiempo real
    LaunchedEffect(requestId) {
        db.collection("requests").document(requestId)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                request = snapshot?.toObject(RequestModel::class.java)

                val interesados = snapshot?.get("interestedTechnicians") as? List<String> ?: emptyList()
                if (interesados.isNotEmpty()) {
                    val listaTemp = mutableListOf<UserModel>()
                    var pendientes = interesados.size
                    interesados.forEach { techId ->
                        db.collection("users").document(techId).get()
                            .addOnSuccessListener { doc ->
                                doc.toObject(UserModel::class.java)?.let { listaTemp.add(it.copy(uid = techId)) }
                                pendientes--
                                if (pendientes == 0) tecnicosInteresados = listaTemp.toList()
                            }
                            .addOnFailureListener {
                                pendientes--
                                if (pendientes == 0) tecnicosInteresados = listaTemp.toList()
                            }
                    }
                    interesados.forEach { techId ->
                        db.collection("requests")
                            .whereEqualTo("technicianId", techId)
                            .whereEqualTo("status", "completada")
                            .get()
                            .addOnSuccessListener { snap ->
                                trabajosRealizadosMap = trabajosRealizadosMap + (techId to snap.size())
                            }
                    }
                } else {
                    tecnicosInteresados = emptyList()
                }

                val techId = snapshot?.getString("technicianId") ?: ""
                if (techId.isNotEmpty()) {
                    db.collection("users").document(techId).get()
                        .addOnSuccessListener { doc -> technician = doc.toObject(UserModel::class.java) }
                }
            }

        // Consultar si ya existe calificación del cliente para esta solicitud
        db.collection("reviews")
            .whereEqualTo("requestId", requestId)
            .whereEqualTo("clientId", uid)
            .get()
            .addOnSuccessListener { snap ->
                reviewExistente = snap.documents.firstOrNull()?.toObject(ReviewModel::class.java)
            }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                clienteName = doc.getString("name") ?: ""
            }
    }

    fun cancelRequest() {
        db.collection("requests").document(requestId)
            .update(mapOf("status" to "cancelada", "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                // Notificar a todos los técnicos interesados
                tecnicosInteresados.forEach { tecnico ->
                    notificationsRepo.crearNotificacion(
                        userId = tecnico.uid,
                        titulo = cancelNotificationTitle,
                        cuerpo = cancelNotificationBody,
                        tipo = "cliente_cancelo",
                        requestId = requestId
                    )
                }
                // Notificar también al técnico asignado si ya había uno
                val techId = request?.technicianId ?: ""
                if (techId.isNotEmpty()) {
                    notificationsRepo.crearNotificacion(
                        userId = techId,
                        titulo = cancelNotificationTitle,
                        cuerpo = cancelNotificationNamedBody,
                        tipo = "tecnico_cancelo",
                        requestId = requestId
                    )
                }
                navController.navigate(Routes.HOME_CLIENT) {
                    popUpTo(Routes.HOME_CLIENT) { inclusive = true }
                }
            }
    }

    fun openWhatsApp(phone: String) {
        val number = phone.replace(Regex("[^0-9]"), "")
        val fullNumber = if (number.startsWith("51")) number else "51$number"
        val message = whatsappMessage
        val uri = Uri.parse("https://wa.me/$fullNumber?text=${Uri.encode(message)}")
        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (e: Exception) { }
    }

    fun elegirTecnico(tecnicoElegidoId: String) {
        eligiendoTecnicoId = tecnicoElegidoId
        db.collection("requests").document(requestId)
            .update(mapOf("status" to "aceptada", "technicianId" to tecnicoElegidoId, "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                notificationsRepo.crearNotificacion(
                    userId = tecnicoElegidoId,
                    titulo = chosenNotificationTitle,
                    cuerpo = chosenNotificationBody,
                    tipo = "tecnico_elegido",
                    requestId = requestId
                )
                tecnicosInteresados.filter { it.uid != tecnicoElegidoId }.forEach { tecnico ->
                    notificationsRepo.crearNotificacion(
                        userId = tecnico.uid,
                        titulo = otherChosenNotificationTitle,
                        cuerpo = otherChosenNotificationBody,
                        tipo = "tecnico_rechazado",
                        requestId = requestId
                    )
                }
                eligiendoTecnicoId = ""
            }
            .addOnFailureListener { eligiendoTecnicoId = "" }
    }

    // Cliente confirma que el trabajo fue completado → abre ventana flotante para  calificación
    fun confirmarCompletado() {
        db.collection("requests").document(requestId)
            .update(mapOf("status" to "completada", "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                showConfirmarCompletadoDialog = false
                showRatingModal = true
            }
    }

    // Cliente rechaza que el trabajo fue completado → notifica al técnico
    fun rechazarCompletado() {
        db.collection("requests").document(requestId)
            .update(mapOf("status" to "aceptada", "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                request?.technicianId?.let { techId ->
                    notificationsRepo.crearNotificacion(
                        userId = techId,
                        titulo = notConfirmedNotificationTitle,
                        cuerpo = notConfirmedNotificationBody,
                        tipo = "completado_rechazado",
                        requestId = requestId
                    )
                }
                showRechazarCompletadoDialog = false
            }
    }

    // Cliente confirma proceso sin continuar
    /*fun confirmarSinContinuar() {
        db.collection("requests").document(requestId)
            .update(mapOf("status" to "sin_continuar", "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                request?.technicianId?.let { techId ->
                    notificationsRepo.crearNotificacion(
                        userId = techId,
                        titulo = notContinuedNotificationTitle,
                        cuerpo = notContinuedNotificationBody,
                        tipo = "sin_continuar_confirmado",
                        requestId = requestId
                    )
                }
                showConfirmarSinContinuarDialog = false
                navController.navigate(Routes.HISTORY) {
                    popUpTo(Routes.HOME_CLIENT) { inclusive = false }
                }
            }
    }*/

    // Enviar calificación y redirigir al historial
    fun submitRating() {
        if (selectedStars == 0) { starsError = selectRatingError; return }
        ratingLoading = true
        val techId = request?.technicianId ?: ""
        val reviewId = UUID.randomUUID().toString()
        val review = ReviewModel(
            reviewId = reviewId,
            requestId = requestId,
            clientId = uid,
            technicianId = techId,
            stars = selectedStars,
            comment = ratingComment.trim(),
            createdAt = System.currentTimeMillis()
        )
        db.collection("reviews").document(reviewId).set(review)
            .addOnSuccessListener {
                // Actualizar promedio del técnico
                if (techId.isNotEmpty()) {
                    db.collection("reviews").whereEqualTo("technicianId", techId).get()
                        .addOnSuccessListener { snapshot ->
                            val reviews = snapshot.documents.mapNotNull { it.toObject(ReviewModel::class.java) }
                            val average = reviews.map { it.stars }.average().toFloat()
                            db.collection("users").document(techId).update("rating", average)
                        }
                    // Notificar al técnico que fue calificado
                    notificationsRepo.crearNotificacion(
                        userId = techId,
                        titulo = ratedNotificationTitle,
                        cuerpo = ratedNotificationBody,
                        tipo = "nueva_solicitud",
                        requestId = requestId
                    )
                }
                ratingLoading = false
                showRatingModal = false
                reviewExistente = review
            }
            .addOnFailureListener { ratingLoading = false }
    }

    // ── Diálogo cancelar solicitud
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.tracking_cancel_request), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.tracking_cancel_request_question)) },
            confirmButton = {
                TextButton(onClick = { cancelRequest() }, colors = ButtonDefaults.textButtonColors(contentColor = Error)) {
                    Text(stringResource(R.string.tracking_yes_cancel), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text(stringResource(R.string.tracking_keep_request)) } }
        )
    }

    // ── Diálogo: confirmar trabajo completado
    if (showConfirmarCompletadoDialog) {
        AlertDialog(
            onDismissRequest = { /* no cerrar tocando afuera */ },
            title = { Text(stringResource(R.string.tracking_completed_question), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.tracking_completed_description)) },
            confirmButton = {
                Button(onClick = { confirmarCompletado() }, colors = ButtonDefaults.buttonColors(containerColor = Success)) {
                    Text(stringResource(R.string.tracking_yes_confirm), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmarCompletadoDialog = false; showRechazarCompletadoDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) { Text(stringResource(R.string.tracking_no_not_finished)) }
            }
        )
    }

    // ── Diálogo: rechazar completado
    if (showRechazarCompletadoDialog) {
        AlertDialog(
            onDismissRequest = { showRechazarCompletadoDialog = false },
            title = { Text(stringResource(R.string.tracking_not_completed_question), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.tracking_not_completed_notice)) },
            confirmButton = {
                Button(onClick = { rechazarCompletado() }, colors = ButtonDefaults.buttonColors(containerColor = Error)) {
                    Text(stringResource(R.string.client_confirm), color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showRechazarCompletadoDialog = false }) { Text(stringResource(R.string.client_cancel)) } }
        )
    }

    // ── Diálogo: confirmar sin continuar
    /*if (showConfirmarSinContinuarDialog) {
        AlertDialog(
            onDismissRequest = { /* no cerrar tocando afuera */ },
            title = { Text("El técnico no puede continuar", fontWeight = FontWeight.Bold) },
            text = { Text("El técnico indicó que no puede continuar con el servicio. ¿Confirmas esto?") },
            confirmButton = {
                Button(onClick = { confirmarSinContinuar() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text(stringResource(R.string.tracking_yes_confirm), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmarSinContinuarDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) { Text("No, el técnico sí puede") }
            }
        )
    }*/

    // Modal calificación
    if (showRatingModal) {
        Dialog(onDismissRequest = { /* no cerrar sin calificar */ }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⭐", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.tracking_rate_service), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    technician?.let { tech ->
                        Text(stringResource(R.string.tracking_how_was_experience_with, tech.name), style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // Estrellas
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = stringResource(R.string.tracking_star_description, star),
                                tint = if (star <= selectedStars) Warning else TextHint,
                                modifier = Modifier.size(44.dp).clickable { selectedStars = star; starsError = "" }
                            )
                        }
                    }

                    if (selectedStars > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (selectedStars) {
                                1 -> stringResource(R.string.tracking_very_bad)
                                2 -> stringResource(R.string.tracking_bad)
                                3 -> stringResource(R.string.tracking_regular)
                                4 -> stringResource(R.string.tracking_good)
                                5 -> stringResource(R.string.tracking_excellent)
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = when (selectedStars) { 1, 2 -> Error; 3 -> Warning; else -> Success },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (starsError.isNotEmpty()) {
                        Text(starsError, color = Error, style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Comentario
                    OutlinedTextField(
                        value = ratingComment,
                        onValueChange = { if (it.length <= 300) ratingComment = it },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        placeholder = { Text(stringResource(R.string.tracking_optional_comment), color = TextHint) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = CardBorder)
                    )
                    Text("${ratingComment.length}/300", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.align(Alignment.End))

                    Spacer(modifier = Modifier.height(16.dp))

                    HomefixButton(text = stringResource(R.string.tracking_send_rating), onClick = { submitRating() }, isLoading = ratingLoading)

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            navController.navigate(Routes.HISTORY) {
                                popUpTo(Routes.HOME_CLIENT) { inclusive = false }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.tracking_skip_now), color = TextSecondary)
                    }
                }
            }
        }
    }

    // Dialog foto expandida con zoom
    fotoExpandidaUrl?.let { url ->
        Dialog(onDismissRequest = { fotoExpandidaUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { fotoExpandidaUrl = null }
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = stringResource(R.string.client_photos),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)), // Bordes curvos
                    contentScale = ContentScale.FillWidth
                )

                IconButton(
                    onClick = { fotoExpandidaUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.client_close),
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Loading
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val req = request ?: return

    // Mostrar diálogos automáticamente cuando el status cambia
    LaunchedEffect(req.status) {
        when (req.status) {
            "pendiente_confirmacion"  -> showConfirmarCompletadoDialog = true
            // "pendiente_sin_continuar" -> showConfirmarSinContinuarDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.client_back), tint = TextPrimary)
                    }
                    Text(stringResource(R.string.tracking_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
                if (req.status == "pendiente" || req.status == "en_revision") {
                    Button(
                        onClick = { showCancelDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Error),
                        border = BorderStroke(1.dp, Error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.client_cancel),
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de progreso
            StatusProgressBar(status = req.status)

            // Técnicos interesados — pendiente o en_revision
            if ((req.status == "pendiente" || req.status == "en_revision") && tecnicosInteresados.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    pluralStringResource(
                        R.plurals.tracking_interested_count,
                        tecnicosInteresados.size,
                        tecnicosInteresados.size
                    ), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                tecnicosInteresados.forEach { tecnico ->
                    TecnicoInteresadoCard(
                        tecnico = tecnico,
                        eligiendoId = eligiendoTecnicoId,
                        trabajosRealizados = trabajosRealizadosMap[tecnico.uid] ?: 0,
                        onElegir = { elegirTecnico(tecnico.uid) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Detalle
            Spacer(modifier = Modifier.height(20.dp))
            Text(stringResource(R.string.tracking_detail), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Badges — tipo de servicio y urgente
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                // MODIFICADO - MULTIDIOMA:
                                trackingServiceLabel(req.serviceType),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (req.isUrgent) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Error.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = Error,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        stringResource(R.string.client_urgent),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = CardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Descripción
                    Text(
                        stringResource(R.string.client_description),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        req.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Dirección
                    if (req.address.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.client_address),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(req.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    // Referencia
                    if (req.reference.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${stringResource(R.string.client_reference)}: ${req.reference}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Fecha
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(req.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    // Imágenes — mostrar hasta 2
                    if (req.imageUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.client_photos),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val urls = req.imageUrls.first().split(",").filter { it.isNotEmpty() }
                        if (urls.size == 1) {
                            AsyncImage(
                                model = urls[0],
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { fotoExpandidaUrl = urls[0] },
                                contentScale = ContentScale.Crop
                            )
                        } else if (urls.size >= 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                urls.take(2).forEach { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { fotoExpandidaUrl = url },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Técnico asignado
            if (technician != null && req.status != "pendiente" && req.status != "en_revision") {
                Spacer(modifier = Modifier.height(20.dp))
                Text(stringResource(R.string.tracking_assigned_technician), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                TecnicoAsignadoCard(technician = technician!!, onWhatsApp = { openWhatsApp(technician!!.whatsapp) })
            }

            // Completada
            if (req.status == "completada") {
                Spacer(modifier = Modifier.height(20.dp))

                // Fecha de finalización
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.tracking_service_completed),
                                style = MaterialTheme.typography.titleSmall,
                                color = Success,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(
                                    R.string.tracking_completed_on,
                                    java.text.SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm",
                                        java.util.Locale.getDefault()
                                    ).format(java.util.Date(req.updatedAt))
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Calificación dada o botón para calificar
                Spacer(modifier = Modifier.height(12.dp))

                if (reviewExistente != null) {
                    // Ya calificó — mostrar la calificación dada
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.tracking_your_rating),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..5).forEach { star ->
                                    Icon(
                                        imageVector = if (star <= (reviewExistente?.stars ?: 0)) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (star <= (reviewExistente?.stars ?: 0)) Warning else TextHint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${reviewExistente?.stars}/5",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (!reviewExistente?.comment.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "\"${reviewExistente?.comment}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(
                                    R.string.tracking_rated_on,
                                    java.text.SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm",
                                        java.util.Locale.getDefault()
                                    ).format(java.util.Date(reviewExistente?.createdAt ?: 0L))
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    // No ha calificado — mostrar botón
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(R.string.tracking_how_was_experience),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.tracking_rating_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showRatingModal = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.tracking_rate_service), color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // Sin continuar
            if (req.status == "sin_continuar") {
                Spacer(modifier = Modifier.height(20.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.08f))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.tracking_process_not_continued), style = MaterialTheme.typography.titleMedium, color = Error, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.tracking_technician_could_not_continue), style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)

                        // Fecha en que el técnico marcó que no podía continuar
                        if (req.technicianCanceledAt > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                stringResource(
                                    R.string.tracking_technician_reported_date,
                                    java.text.SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm",
                                        java.util.Locale.getDefault()
                                    ).format(java.util.Date(req.technicianCanceledAt))
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        // Fecha en que el cliente confirmó (updatedAt se actualiza en confirmarSinContinuar())
                        Text(
                            stringResource(
                                R.string.tracking_confirmed_date,
                                java.text.SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date(req.updatedAt))
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Componentes auxiliares
// ─────────────────────────────────────────────────────────────

@Composable
fun TecnicoInteresadoCard(
    tecnico: UserModel,
    eligiendoId: String,
    trabajosRealizados: Int,
    onElegir: () -> Unit
) {
    var bioExpandida by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header — foto + nombre + rating
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                // Foto de perfil real o inicial
                if (tecnico.selfieUrl.isNotEmpty()) {
                    AsyncImage(
                        model = tecnico.selfieUrl,
                        contentDescription = stringResource(R.string.technicians_photo),
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(26.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        color = TechnicianColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                tecnico.name.firstOrNull()?.toString()?.uppercase() ?: "T",
                                style = MaterialTheme.typography.titleMedium,
                                color = TechnicianColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${tecnico.name} ${tecnico.lastName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Rating
                    if (tecnico.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            (1..5).forEach { star ->
                                Icon(
                                    imageVector = if (star <= tecnico.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (star <= tecnico.rating) Warning else TextHint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${"%.1f".format(tecnico.rating)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.client_no_ratings),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    // Distrito
                    if (tecnico.district.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                tecnico.district,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Todas las especialidades
            if (tecnico.specialties.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        trackingSpecialtiesLabel(tecnico.specialties),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Años de experiencia
            if (tecnico.yearsExp > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkHistory, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        pluralStringResource(
                            R.plurals.tracking_years_experience,
                            tecnico.yearsExp,
                            tecnico.yearsExp
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Trabajos realizados
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (trabajosRealizados == 0) {
                        stringResource(R.string.client_new_on_platform)
                    } else {
                        pluralStringResource(
                            R.plurals.tracking_completed_jobs,
                            trabajosRealizados,
                            trabajosRealizados
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (trabajosRealizados > 0) Success else TextSecondary,
                    fontWeight = if (trabajosRealizados > 0) FontWeight.Medium else FontWeight.Normal
                )
            }

            // Bio con "Ver más"
            if (tecnico.bio.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    tecnico.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = if (bioExpandida) Int.MAX_VALUE else 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (tecnico.bio.length > 80) {
                    Text(
                        text = if (bioExpandida) stringResource(R.string.client_view_less) else stringResource(R.string.client_view_more),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { bioExpandida = !bioExpandida }
                            .padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onElegir,
                enabled = eligiendoId.isEmpty(),
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success)
            ) {
                if (eligiendoId == tecnico.uid) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.tracking_choose_technician), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun TecnicoAsignadoCard(technician: UserModel, onWhatsApp: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                // Foto de perfil real o inicial
                if (technician.selfieUrl.isNotEmpty()) {
                    AsyncImage(
                        model = technician.selfieUrl,
                        contentDescription = stringResource(R.string.technicians_photo),
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(26.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        color = TechnicianColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                technician.name.firstOrNull()?.toString()?.uppercase() ?: "T",
                                style = MaterialTheme.typography.titleMedium,
                                color = TechnicianColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${technician.name} ${technician.lastName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (technician.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            (1..5).forEach { star ->
                                Icon(
                                    imageVector = if (star <= technician.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (star <= technician.rating) Warning else TextHint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${"%.1f".format(technician.rating)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    if (technician.district.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(technician.district, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Todas las especialidades
            if (technician.specialties.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        trackingSpecialtiesLabel(technician.specialties),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Años de experiencia
            if (technician.yearsExp > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkHistory, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        pluralStringResource(
                            R.plurals.tracking_years_experience,
                            technician.yearsExp,
                            technician.yearsExp
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (technician.whatsapp.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onWhatsApp,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.tracking_contact_whatsapp), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun StatusProgressBar(status: String) {
    // "en_camino" eliminado del flujo
    // MODIFICADO - MULTIDIOMA:
    // Los códigos internos de estado se mantienen; solo cambian sus etiquetas.
    val steps = listOf(
        "pendiente" to stringResource(R.string.status_pending),
        "en_revision" to stringResource(R.string.status_under_review),
        "aceptada" to stringResource(R.string.status_accepted),
        "completada" to stringResource(R.string.status_completed)
    )

    // Estados intermedios de confirmación se mapean visualmente a "aceptada"
    val statusVisual = when (status) {
        "pendiente_confirmacion", "pendiente_sin_continuar" -> "aceptada"
        "sin_continuar" -> "completada"
        else -> status
    }

    val currentIndex = steps.indexOfFirst { it.first == statusVisual }.let { if (it == -1) 0 else it }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.tracking_service_status), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            steps.forEachIndexed { index, (_, label) ->
                val isDone = index < currentIndex
                val isCurrent = index == currentIndex

                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = when { isDone -> Success; isCurrent -> Primary; else -> SurfaceVariant }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isDone) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                else Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = if (isCurrent) Color.White else TextSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (index < steps.size - 1) {
                            Box(modifier = Modifier.width(2.dp).height(28.dp).background(color = if (isDone) Success else CardBorder, shape = RoundedCornerShape(1.dp)))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when { isDone -> Success; isCurrent -> Primary; else -> TextSecondary },
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isCurrent) {
                            Text(getStatusDescription(status), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        if (index < steps.size - 1) Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

// MODIFICADO - MULTIDIOMA:
// Traduce únicamente la descripción visible del estado.
@Composable
fun getStatusDescription(status: String): String = when (status) {
    "pendiente" ->
        stringResource(R.string.tracking_status_pending_description)
    "en_revision" ->
        stringResource(R.string.tracking_status_review_description)
    "aceptada" ->
        stringResource(R.string.tracking_status_accepted_description)
    "pendiente_confirmacion" ->
        stringResource(R.string.tracking_status_pending_confirmation_description)
    "pendiente_sin_continuar" ->
        stringResource(R.string.tracking_status_pending_not_continue_description)
    "completada" ->
        stringResource(R.string.tracking_status_completed_description)
    "sin_continuar" ->
        stringResource(R.string.tracking_status_not_continued_description)
    "cancelada" ->
        stringResource(R.string.tracking_status_canceled_description)
    else -> ""
}