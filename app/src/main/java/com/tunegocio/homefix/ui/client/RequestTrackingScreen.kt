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
import java.util.UUID

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

    // Confirmación completado
    var showConfirmarCompletadoDialog by remember { mutableStateOf(false) }
    var showRechazarCompletadoDialog by remember { mutableStateOf(false) }

    //  Confirmación sin continuar
    var showConfirmarSinContinuarDialog by remember { mutableStateOf(false) }

    // Modal de calificación inline
    var showRatingModal by remember { mutableStateOf(false) }
    var selectedStars by remember { mutableStateOf(0) }
    var ratingComment by remember { mutableStateOf("") }
    var ratingLoading by remember { mutableStateOf(false) }
    var starsError by remember { mutableStateOf("") }

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
    }

    fun cancelRequest() {
        db.collection("requests").document(requestId)
            .update(mapOf("status" to "cancelada", "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                navController.navigate(Routes.HOME_CLIENT) {
                    popUpTo(Routes.HOME_CLIENT) { inclusive = true }
                }
            }
    }

    fun openWhatsApp(phone: String) {
        val number = phone.replace(Regex("[^0-9]"), "")
        val fullNumber = if (number.startsWith("51")) number else "51$number"
        val message = "Hola, soy el cliente de HomeFix. ¿Cómo va el servicio?"
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
                    titulo = "¡Te eligieron!",
                    cuerpo = "El cliente eligió tu propuesta para ${request?.serviceType ?: ""}. Prepárate para ir.",
                    tipo = "tecnico_elegido",
                    requestId = requestId
                )
                tecnicosInteresados.filter { it.uid != tecnicoElegidoId }.forEach { tecnico ->
                    notificationsRepo.crearNotificacion(
                        userId = tecnico.uid,
                        titulo = "Solicitud asignada a otro",
                        cuerpo = "El cliente eligió a otro técnico para esta solicitud.",
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
                        titulo = "El cliente no confirmó el trabajo",
                        cuerpo = "El cliente indicó que el trabajo no fue completado. Revisa la situación.",
                        tipo = "completado_rechazado",
                        requestId = requestId
                    )
                }
                showRechazarCompletadoDialog = false
            }
    }

    // Cliente confirma proceso sin continuar
    fun confirmarSinContinuar() {
        db.collection("requests").document(requestId)
            .update(mapOf("status" to "sin_continuar", "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                request?.technicianId?.let { techId ->
                    notificationsRepo.crearNotificacion(
                        userId = techId,
                        titulo = "Proceso confirmado como no continuado",
                        cuerpo = "El cliente confirmó que el proceso no continuó.",
                        tipo = "sin_continuar_confirmado",
                        requestId = requestId
                    )
                }
                showConfirmarSinContinuarDialog = false
                navController.navigate(Routes.HISTORY) {
                    popUpTo(Routes.HOME_CLIENT) { inclusive = false }
                }
            }
    }

    // Enviar calificación y redirigir al historial
    fun submitRating() {
        if (selectedStars == 0) { starsError = "Selecciona una calificación"; return }
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
                }
                ratingLoading = false
                navController.navigate(Routes.HISTORY) {
                    popUpTo(Routes.HOME_CLIENT) { inclusive = false }
                }
            }
            .addOnFailureListener { ratingLoading = false }
    }

    // ── Diálogo cancelar solicitud
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar solicitud", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro que deseas cancelar esta solicitud?") },
            confirmButton = {
                TextButton(onClick = { cancelRequest() }, colors = ButtonDefaults.textButtonColors(contentColor = Error)) {
                    Text("Sí, cancelar", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text("No, mantener") } }
        )
    }

    // ── Diálogo: confirmar trabajo completado
    if (showConfirmarCompletadoDialog) {
        AlertDialog(
            onDismissRequest = { /* no cerrar tocando afuera */ },
            title = { Text("¿El trabajo fue completado?", fontWeight = FontWeight.Bold) },
            text = { Text("El técnico indicó que terminó el trabajo. ¿Confirmas esto?") },
            confirmButton = {
                Button(onClick = { confirmarCompletado() }, colors = ButtonDefaults.buttonColors(containerColor = Success)) {
                    Text("Sí, confirmar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmarCompletadoDialog = false; showRechazarCompletadoDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) { Text("No, no terminó") }
            }
        )
    }

    // ── Diálogo: rechazar completado
    if (showRechazarCompletadoDialog) {
        AlertDialog(
            onDismissRequest = { showRechazarCompletadoDialog = false },
            title = { Text("¿El trabajo no fue completado?", fontWeight = FontWeight.Bold) },
            text = { Text("Se notificará al técnico que el trabajo no fue aceptado como completado.") },
            confirmButton = {
                Button(onClick = { rechazarCompletado() }, colors = ButtonDefaults.buttonColors(containerColor = Error)) {
                    Text("Confirmar", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showRechazarCompletadoDialog = false }) { Text("Cancelar") } }
        )
    }

    // ── Diálogo: confirmar sin continuar
    if (showConfirmarSinContinuarDialog) {
        AlertDialog(
            onDismissRequest = { /* no cerrar tocando afuera */ },
            title = { Text("El técnico no puede continuar", fontWeight = FontWeight.Bold) },
            text = { Text("El técnico indicó que no puede continuar con el servicio. ¿Confirmas esto?") },
            confirmButton = {
                Button(onClick = { confirmarSinContinuar() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("Sí, confirmar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmarSinContinuarDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) { Text("No, el técnico sí puede") }
            }
        )
    }

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
                    Text("Califica el servicio", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    technician?.let { tech ->
                        Text("¿Cómo fue tu experiencia con ${tech.name}?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // Estrellas
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Estrella $star",
                                tint = if (star <= selectedStars) Warning else TextHint,
                                modifier = Modifier.size(44.dp).clickable { selectedStars = star; starsError = "" }
                            )
                        }
                    }

                    if (selectedStars > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (selectedStars) {
                                1 -> "Muy malo 😞"; 2 -> "Malo 😕"; 3 -> "Regular 😐"
                                4 -> "Bueno 😊"; 5 -> "Excelente 🤩"; else -> ""
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
                        placeholder = { Text("Comentario opcional...", color = TextHint) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = CardBorder)
                    )
                    Text("${ratingComment.length}/300", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.align(Alignment.End))

                    Spacer(modifier = Modifier.height(16.dp))

                    HomefixButton(text = "Enviar calificación", onClick = { submitRating() }, isLoading = ratingLoading)

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            navController.navigate(Routes.HISTORY) {
                                popUpTo(Routes.HOME_CLIENT) { inclusive = false }
                            }
                        }
                    ) {
                        Text("Omitir por ahora", color = TextSecondary)
                    }
                }
            }
        }
    }

    // ── Loading
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
            "pendiente_sin_continuar" -> showConfirmarSinContinuarDialog = true
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                    Text("Mi solicitud", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
                if (req.status == "pendiente" || req.status == "en_revision") {
                    TextButton(onClick = { showCancelDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = Error)) {
                        Text("Cancelar", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de progreso
            StatusProgressBar(status = req.status)

            // Técnicos interesados — pendiente o en_revision
            if ((req.status == "pendiente" || req.status == "en_revision") && tecnicosInteresados.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Técnicos interesados (${tecnicosInteresados.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
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
            Text("Detalle", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
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
                                req.serviceType,
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
                                        "Urgente",
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
                        "Descripción",
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
                            "Dirección",
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
                                "Ref: ${req.reference}",
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
                            "Fotos",
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
                                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
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
                                        modifier = Modifier.weight(1f).height(140.dp).clip(RoundedCornerShape(12.dp)),
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
                Text("Técnico asignado", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                TecnicoAsignadoCard(technician = technician!!, onWhatsApp = { openWhatsApp(technician!!.whatsapp) })
            }

            // Completada
            if (req.status == "completada") {
                Spacer(modifier = Modifier.height(20.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.08f))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Servicio completado", style = MaterialTheme.typography.titleMedium, color = Success, fontWeight = FontWeight.SemiBold)
                        Text("¿Cómo fue tu experiencia?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate(Routes.rating(requestId)) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calificar servicio", color = Color.White, style = MaterialTheme.typography.labelLarge)
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
                        Text("Proceso no continuado", style = MaterialTheme.typography.titleMedium, color = Error, fontWeight = FontWeight.SemiBold)
                        Text("El técnico no pudo continuar con este servicio.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)

                        // Fecha en que el técnico marcó que no podía continuar
                        if (req.technicianCanceledAt > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Técnico reportó: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(req.technicianCanceledAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        // Fecha en que el cliente confirmó (updatedAt se actualiza en confirmarSinContinuar())
                        Text(
                            "Confirmado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(req.updatedAt))}",
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
                        contentDescription = "Foto de ${tecnico.name}",
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
                        tecnico.name,
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
                            "Sin calificaciones aún",
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
                        tecnico.specialties.joinToString(", "),
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
                        "${tecnico.yearsExp} año${if (tecnico.yearsExp != 1) "s" else ""} de experiencia",
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
                    if (trabajosRealizados == 0) "Nuevo en la plataforma"
                    else "$trabajosRealizados servicio${if (trabajosRealizados != 1) "s" else ""} realizado${if (trabajosRealizados != 1) "s" else ""}",
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
                        text = if (bioExpandida) "Ver menos" else "Ver más",
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
                    Text("Elegir este técnico", color = Color.White, style = MaterialTheme.typography.labelLarge)
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
                        contentDescription = "Foto de ${technician.name}",
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
                        technician.name,
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
                        technician.specialties.joinToString(", "),
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
                        "${technician.yearsExp} año${if (technician.yearsExp != 1) "s" else ""} de experiencia",
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
                    Text("Contactar por WhatsApp", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun StatusProgressBar(status: String) {
    // "en_camino" eliminado del flujo
    val steps = listOf(
        "pendiente"   to "Pendiente",
        "en_revision" to "En revisión",
        "aceptada"    to "Aceptada",
        "completada"  to "Completada"
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
            Text("Estado del servicio", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
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

fun getStatusDescription(status: String): String = when (status) {
    "pendiente"               -> "Esperando que un técnico acepte..."
    "en_revision"             -> "Hay técnicos interesados, elige uno"
    "aceptada"                -> "¡Elegiste un técnico!"
    "pendiente_confirmacion"  -> "El técnico terminó, confirma el trabajo"
    "pendiente_sin_continuar" -> "El técnico no puede continuar, confirma"
    "completada"              -> "El servicio fue completado exitosamente"
    "sin_continuar"           -> "El proceso no pudo completarse"
    "cancelada"               -> "La solicitud fue cancelada"
    else                      -> ""
}