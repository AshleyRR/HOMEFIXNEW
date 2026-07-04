package com.tunegocio.homefix.ui.technician

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.NotificationsRepository
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.theme.*
import com.tunegocio.homefix.data.model.ReviewModel

import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.clickable


import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.Icon
import androidx.compose.foundation.BorderStroke


import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Block



@Composable
fun RequestDetailScreen(
    navController: NavController,
    requestId: String
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val technicianId = auth.currentUser?.uid ?: ""

    // ── Colores dinámicos (igual que EarningsScreen)
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val primaryColor = MaterialTheme.colorScheme.primary

    var request by remember { mutableStateOf<RequestModel?>(null) }
    var client by remember { mutableStateOf<UserModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var showConfirmInterestDialog by remember { mutableStateOf(false) }
    var showCancelReasonDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var cancelReasonError by remember { mutableStateOf("") }
    var showCompletadoDialog by remember { mutableStateOf(false) }
    var showSinContinuarDialog by remember { mutableStateOf(false) }

    val notificationsRepo = remember { NotificationsRepository() }

    //VER CALIFICACIONES Y RESEÑAS
    var review by remember { mutableStateOf<ReviewModel?>(null) }
    var sinContinuarMotivo by remember { mutableStateOf("") }
    var sinContinuarMotivoError by remember { mutableStateOf("") }

    LaunchedEffect(requestId) {
        db.collection("requests").document(requestId)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                request = snapshot?.toObject(RequestModel::class.java)
                //ADICIONAL
                if (request?.status == "completada") {
                    db.collection("reviews")
                        .whereEqualTo("requestId", requestId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { result ->
                            review = result.documents.firstOrNull()?.toObject(ReviewModel::class.java)
                        }
                }

                request?.clientId?.let { clientId ->
                    if (clientId.isNotEmpty()) {
                        db.collection("users").document(clientId).get()
                            .addOnSuccessListener { clientDoc ->
                                client = clientDoc.toObject(UserModel::class.java)
                            }
                    }
                }
            }
    }

    fun acceptRequest() {
        actionLoading = true
        val req = request ?: return
        db.collection("requests").document(requestId).get()
            .addOnSuccessListener { snap ->
                val interesadosActuales = snap.get("interestedTechnicians") as? List<*>
                val esPrimero = interesadosActuales.isNullOrEmpty()
                val updates = mutableMapOf<String, Any>(
                    "interestedTechnicians" to FieldValue.arrayUnion(technicianId),
                    "updatedAt" to System.currentTimeMillis()
                )
                if (esPrimero) updates["status"] = "en_revision"
                db.collection("requests").document(requestId)
                    .update(updates)
                    .addOnSuccessListener {
                        notificationsRepo.crearNotificacion(
                            userId = req.clientId,
                            titulo = "¡Un técnico está interesado!",
                            cuerpo = "Alguien quiere atender tu solicitud de ${req.serviceType}. Revísala para elegir.",
                            tipo = "tecnico_aceptado",
                            requestId = requestId
                        )
                        actionLoading = false
                    }
                    .addOnFailureListener {
                        actionLoading = false
                        errorMessage = "Error al registrar interés"
                    }
            }
            .addOnFailureListener {
                actionLoading = false
                errorMessage = "Error al verificar la solicitud"
            }
    }

    fun cancelInterest() {
        if (cancelReason.isBlank()) { cancelReasonError = "Por favor escribe el motivo"; return }
        actionLoading = true
        val req = request ?: return
        db.collection("requests").document(requestId)
            .update(mapOf("interestedTechnicians" to FieldValue.arrayRemove(technicianId), "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                db.collection("requests").document(requestId).get()
                    .addOnSuccessListener { snap ->
                        val interesados = snap.get("interestedTechnicians") as? List<*>
                        if (interesados.isNullOrEmpty()) {
                            db.collection("requests").document(requestId).update("status", "pendiente")
                        }
                    }
                notificationsRepo.crearNotificacion(
                    userId = req.clientId,
                    titulo = "Un técnico canceló su interés",
                    cuerpo = "Motivo: $cancelReason",
                    tipo = "tecnico_cancelo",
                    requestId = requestId
                )
                actionLoading = false
                showCancelReasonDialog = false
                cancelReason = ""
                navController.popBackStack()
            }
            .addOnFailureListener { actionLoading = false; errorMessage = "Error al cancelar" }
    }

    fun solicitarCompletado() {
        actionLoading = true
        val req = request ?: return
        db.collection("requests").document(requestId)
            .update(mapOf("status" to "pendiente_confirmacion", "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                notificationsRepo.crearNotificacion(
                    userId = req.clientId,
                    titulo = "El técnico terminó el trabajo",
                    cuerpo = "¿Confirmas que el servicio de ${req.serviceType} fue completado?",
                    tipo = "confirmar_completado",
                    requestId = requestId
                )
                actionLoading = false
                showCompletadoDialog = false
            }
            .addOnFailureListener { actionLoading = false; errorMessage = "Error al actualizar" }
    }

    fun solicitarSinContinuar() {
        if (sinContinuarMotivo.isBlank()) { sinContinuarMotivoError = "Por favor escribe el motivo"; return }
        actionLoading = true
        val req = request ?: return
        db.collection("requests").document(requestId)
            .update(mapOf(
                "status" to "sin_continuar", // ← cambia directo a sin_continuar
                "updatedAt" to System.currentTimeMillis(),
                "technicianCanceledAt" to System.currentTimeMillis()
            ))
            .addOnSuccessListener {
                notificationsRepo.crearNotificacion(
                    userId = req.clientId,
                    titulo = "El técnico no puede continuar",
                    cuerpo = "Motivo: $sinContinuarMotivo",
                    tipo = "sin_continuar_confirmado",
                    requestId = requestId
                )
                actionLoading = false
                showSinContinuarDialog = false
                sinContinuarMotivo = ""
                navController.popBackStack()
            }
            .addOnFailureListener { actionLoading = false; errorMessage = "Error al actualizar" }
    }



    fun openWhatsApp(phone: String) {
        val number = phone.replace(Regex("[^0-9]"), "")
        val fullNumber = if (number.startsWith("51")) number else "51$number"
        val message = "Hola, vi tu solicitud en HomeFix y me gustaría ayudarte."
        val uri = Uri.parse("https://wa.me/$fullNumber?text=${Uri.encode(message)}")
        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        catch (e: Exception) { errorMessage = "WhatsApp no está instalado" }
    }

    fun openGoogleMaps(lat: Double, lng: Double, address: String) {
        val query = if (lat != 0.0 && lng != 0.0) "$lat,$lng" else Uri.encode(address)
        val uri = Uri.parse("geo:$lat,$lng?q=$query")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
        try { context.startActivity(intent) }
        catch (e: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$query"))) }
    }

    // ── Diálogo: confirmar interés
    if (showConfirmInterestDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmInterestDialog = false },
            title = { Text("¿Confirmar interés?", fontWeight = FontWeight.Bold, color = textColor) },
            text = { Text("Al confirmar, el cliente verá que estás interesado en atender esta solicitud y podrá elegirte.", color = secondaryText) },
            confirmButton = {
                Button(onClick = { showConfirmInterestDialog = false; acceptRequest() }, colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) {
                    Text("Sí, me interesa", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirmInterestDialog = false }) { Text("Cancelar", color = secondaryText) } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // ── Diálogo: motivo cancelación
    if (showCancelReasonDialog) {
        AlertDialog(
            onDismissRequest = { showCancelReasonDialog = false; cancelReason = ""; cancelReasonError = "" },
            title = { Text("¿Por qué cancelas?", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column {
                    Text("El cliente recibirá una notificación con el motivo.", color = secondaryText, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it; cancelReasonError = "" },
                        placeholder = { Text("Ej: Surgió un inconveniente, estoy lejos...", color = secondaryText) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = outlineColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = primaryColor
                        ),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                    if (cancelReasonError.isNotEmpty()) Text(cancelReasonError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                Button(onClick = { cancelInterest() }, enabled = !actionLoading, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    if (actionLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Confirmar cancelación", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showCancelReasonDialog = false; cancelReason = ""; cancelReasonError = "" }) { Text("Volver", color = secondaryText) } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // Diálogo: trabajo completado
    if (showCompletadoDialog) {
        AlertDialog(
            onDismissRequest = { showCompletadoDialog = false },
            title = { Text("¿Marcar como completado?", fontWeight = FontWeight.Bold, color = textColor) },
            text = { Text("Se notificará al cliente para que confirme que el trabajo fue terminado.", color = secondaryText) },
            confirmButton = {
                Button(onClick = { solicitarCompletado() }, enabled = !actionLoading, colors = ButtonDefaults.buttonColors(containerColor = Success)) {
                    if (actionLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Confirmar", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showCompletadoDialog = false }) { Text("Cancelar", color = secondaryText) } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // ── Diálogo: proceso sin continuar
    if (showSinContinuarDialog) {
        AlertDialog(
            onDismissRequest = { showSinContinuarDialog = false; sinContinuarMotivo = ""; sinContinuarMotivoError = "" },
            title = { Text("¿No puedes continuar?", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column {
                    Text(
                        "El cliente recibirá una notificación con el motivo.",
                        color = secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = sinContinuarMotivo,
                        onValueChange = { sinContinuarMotivo = it; sinContinuarMotivoError = "" },
                        placeholder = { Text("Ej: Surgió un inconveniente, no puedo llegar...", color = secondaryText) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = outlineColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = primaryColor
                        ),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                    if (sinContinuarMotivoError.isNotEmpty()) {
                        Text(sinContinuarMotivoError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { solicitarSinContinuar() },
                    enabled = !actionLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (actionLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Confirmar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSinContinuarDialog = false; sinContinuarMotivo = ""; sinContinuarMotivoError = "" }) {
                    Text("Cancelar", color = secondaryText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // ── Loading
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = primaryColor)
        }
        return
    }


    val req = request ?: return
    val yaMarcoInteres = technicianId in req.interestedTechnicians
    var fotoExpandidaUrl by remember { mutableStateOf<String?>(null) }

    // Dialog foto expandida
    fotoExpandidaUrl?.let { url ->
        Dialog(onDismissRequest = { fotoExpandidaUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { fotoExpandidaUrl = null }
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = "Foto ampliada",
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.FillWidth
                )
                IconButton(
                    onClick = { fotoExpandidaUrl = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = textColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Detalle de solicitud", style = MaterialTheme.typography.headlineMedium, color = textColor, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badges — tipo servicio, urgente, en proceso
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = primaryColor.copy(alpha = 0.1f)) {
                    Text(
                        req.serviceType,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (req.isUrgent) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                            Text("Urgente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (req.status == "aceptada" && req.technicianId == technicianId) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Info.copy(alpha = 0.1f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = Info, modifier = Modifier.size(12.dp))
                            Text("En proceso", style = MaterialTheme.typography.labelSmall, color = Info, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card descripción + dirección + fecha + fotos
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Descripción
                    Text("Descripción", style = MaterialTheme.typography.labelMedium, color = secondaryText, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(req.description, style = MaterialTheme.typography.bodyMedium, color = textColor)

                    // Dirección
                    if (req.address.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Dirección", style = MaterialTheme.typography.labelMedium, color = secondaryText, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(req.address, style = MaterialTheme.typography.bodySmall, color = textColor)
                        }
                        if (req.reference.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = secondaryText, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ref: ${req.reference}", style = MaterialTheme.typography.bodySmall, color = secondaryText)
                            }
                        }
                        if (req.lat != 0.0 || req.address.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { openGoogleMaps(req.lat, req.lng, req.address) },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C7EDE))
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ver en Google Maps", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    // Fecha de creación
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = secondaryText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(req.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText
                        )
                    }

                    // Fotos — hasta 2 con zoom
                    if (req.imageUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Fotos", style = MaterialTheme.typography.labelMedium, color = secondaryText, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        val urls = req.imageUrls.first().split(",").filter { it.isNotEmpty() }
                        if (urls.size == 1) {
                            AsyncImage(
                                model = urls[0],
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
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
                                            .height(160.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Cliente
            client?.let { c ->
                Text("Cliente", style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = ClientColor.copy(alpha = 0.15f)
                            ) {
                                if (c.selfieUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = c.selfieUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(c.name.firstOrNull()?.toString() ?: "C", style = MaterialTheme.typography.titleMedium, color = ClientColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("${c.name} ${c.lastName}", style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.SemiBold)
                                if (c.district.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = secondaryText, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(c.district, style = MaterialTheme.typography.labelSmall, color = secondaryText)
                                    }
                                }
                            }
                        }
                        if (c.phone.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { openWhatsApp(c.phone) },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Contactar por WhatsApp", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botones según estado
            when (req.status) {
                "pendiente", "en_revision" -> {
                    if (yaMarcoInteres) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF17192E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.HourglassBottom, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Solicitud enviada", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("Estás a la espera de que el cliente te elija.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showCancelReasonDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Cancelar solicitud",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    } else {
                        HomefixButton(
                            text = "Me interesa",
                            onClick = { showConfirmInterestDialog = true },
                            isLoading = actionLoading,
                            color = primaryColor,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "No me interesa",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                "aceptada" -> {
                    if (req.technicianId == technicianId) {
                        HomefixButton(
                            text = "Finalizado",
                            onClick = { showCompletadoDialog = true },
                            isLoading = actionLoading,
                            color = Color(0xFF17192E),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showSinContinuarDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Sin continuar",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                "pendiente_confirmacion" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF17192E)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.HourglassBottom, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Solicitud enviada", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Estás a la espera de que el cliente te elija.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                    }
                }

                "pendiente_sin_continuar" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.HourglassBottom, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Esperando confirmación", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                Text("El cliente debe confirmar que el proceso no continuó.", style = MaterialTheme.typography.bodySmall, color = secondaryText)
                            }
                        }
                    }
                }

                "completada" -> {
                    // Badge completado con fecha
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.08f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Servicio completado", style = MaterialTheme.typography.titleSmall, color = Success, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Finalizado el ${java.text.SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", java.util.Locale.getDefault()).format(java.util.Date(req.updatedAt))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryText
                                )
                            }
                        }
                    }

                    // Calificación del cliente
                    review?.let { r ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Calificación del cliente", style = MaterialTheme.typography.titleSmall, color = textColor, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    (1..5).forEach { star ->
                                        Icon(
                                            imageVector = if (star <= r.stars) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = null,
                                            tint = if (star <= r.stars) Warning else TextHint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${r.stars}/5", style = MaterialTheme.typography.bodyMedium, color = textColor, fontWeight = FontWeight.SemiBold)
                                }
                                if (r.comment.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("\"${r.comment}\"", style = MaterialTheme.typography.bodySmall, color = secondaryText)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(r.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryText
                                )
                            }
                        }
                    } ?: run {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.StarBorder, contentDescription = null, tint = secondaryText, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("El cliente aún no ha calificado el servicio.", style = MaterialTheme.typography.bodySmall, color = secondaryText)
                            }
                        }
                    }
                }

                "sin_continuar" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Proceso no continuado", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

