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
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.theme.*

@Composable
fun RequestDetailScreen(
    navController: NavController,
    requestId: String
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val technicianId = auth.currentUser?.uid ?: ""

    var request by remember { mutableStateOf<RequestModel?>(null) }
    var client by remember { mutableStateOf<UserModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // ── Diálogos ──────────────────────────────────────────────
    var showConfirmInterestDialog by remember { mutableStateOf(false) }
    var showCancelReasonDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var cancelReasonError by remember { mutableStateOf("") }
    var showCompletadoDialog by remember { mutableStateOf(false) }
    var showSinContinuarDialog by remember { mutableStateOf(false) }

    val notificationsRepo = remember { NotificationsRepository() }

    // Escucha en tiempo real
    LaunchedEffect(requestId) {
        db.collection("requests").document(requestId)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                request = snapshot?.toObject(RequestModel::class.java)
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

    // ── Acciones ──────────────────────────────────────────────

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

    // Notifica al cliente que el técnico marcó trabajo completado
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

    // Notifica al cliente que el técnico no puede continuar
    fun solicitarSinContinuar() {
        actionLoading = true
        val req = request ?: return
        db.collection("requests").document(requestId)
            .update(mapOf("status" to "pendiente_sin_continuar", "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                notificationsRepo.crearNotificacion(
                    userId = req.clientId,
                    titulo = "El técnico no puede continuar",
                    cuerpo = "El técnico indicó que no puede continuar con el servicio de ${req.serviceType}. ¿Lo confirmas?",
                    tipo = "confirmar_sin_continuar",
                    requestId = requestId
                )
                actionLoading = false
                showSinContinuarDialog = false
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
            title = { Text("¿Confirmar interés?", fontWeight = FontWeight.Bold) },
            text = { Text("Al confirmar, el cliente verá que estás interesado en atender esta solicitud y podrá elegirte.") },
            confirmButton = {
                Button(onClick = { showConfirmInterestDialog = false; acceptRequest() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("Sí, me interesa", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirmInterestDialog = false }) { Text("Cancelar") } }
        )
    }

    // ── Diálogo: motivo cancelación
    if (showCancelReasonDialog) {
        AlertDialog(
            onDismissRequest = { showCancelReasonDialog = false; cancelReason = ""; cancelReasonError = "" },
            title = { Text("¿Por qué cancelas?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("El cliente recibirá una notificación con el motivo.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it; cancelReasonError = "" },
                        placeholder = { Text("Ej: Surgió un inconveniente, estoy lejos...") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = CardBorder),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                    if (cancelReasonError.isNotEmpty()) Text(cancelReasonError, color = Error, style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                Button(onClick = { cancelInterest() }, enabled = !actionLoading, colors = ButtonDefaults.buttonColors(containerColor = Error)) {
                    if (actionLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Confirmar cancelación", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showCancelReasonDialog = false; cancelReason = ""; cancelReasonError = "" }) { Text("Volver") } }
        )
    }

    // ── Diálogo: trabajo completado ───────────────────────────
    if (showCompletadoDialog) {
        AlertDialog(
            onDismissRequest = { showCompletadoDialog = false },
            title = { Text("¿Marcar como completado?", fontWeight = FontWeight.Bold) },
            text = { Text("Se notificará al cliente para que confirme que el trabajo fue terminado.") },
            confirmButton = {
                Button(onClick = { solicitarCompletado() }, enabled = !actionLoading, colors = ButtonDefaults.buttonColors(containerColor = Success)) {
                    if (actionLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Confirmar", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showCompletadoDialog = false }) { Text("Cancelar") } }
        )
    }

    // ── Diálogo: proceso sin continuar ────────────────────────
    if (showSinContinuarDialog) {
        AlertDialog(
            onDismissRequest = { showSinContinuarDialog = false },
            title = { Text("¿No puedes continuar?", fontWeight = FontWeight.Bold) },
            text = { Text("Se notificará al cliente que no puedes continuar con este servicio. El cliente deberá confirmarlo.") },
            confirmButton = {
                Button(onClick = { solicitarSinContinuar() }, enabled = !actionLoading, colors = ButtonDefaults.buttonColors(containerColor = Error)) {
                    if (actionLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Confirmar", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showSinContinuarDialog = false }) { Text("Cancelar") } }
        )
    }

    // ── Loading ───────────────────────────────────────────────
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val req = request ?: return
    val yaMarcoInteres = technicianId in req.interestedTechnicians

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Detalle de solicitud", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chips: tipo, urgencia, en proceso
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = Primary.copy(alpha = 0.1f)) {
                    Text(req.serviceType, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, color = Primary, fontWeight = FontWeight.SemiBold)
                }
                if (req.isUrgent) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Error.copy(alpha = 0.1f)) {
                        Text("⚡ Urgente", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, color = Error, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (req.status == "aceptada" && req.technicianId == technicianId) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Info.copy(alpha = 0.1f)) {
                        Text("🔧 En proceso", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, color = Info, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Foto
            if (req.imageUrls.isNotEmpty()) {
                Text("Foto del problema", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(model = req.imageUrls.first(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Descripción
            Text("Descripción", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SurfaceVariant)) {
                Text(req.description, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ubicación
            Text("Ubicación", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SurfaceVariant)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(req.address.ifEmpty { "Ubicación no especificada" }, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    }
                    if (req.reference.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(req.reference, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    if (req.lat != 0.0 || req.address.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { openGoogleMaps(req.lat, req.lng, req.address) },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver en Google Maps", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cliente
            client?.let { c ->
                Text("Cliente", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBackground), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(22.dp), color = ClientColor.copy(alpha = 0.15f)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(c.name.firstOrNull()?.toString() ?: "C", style = MaterialTheme.typography.titleMedium, color = ClientColor, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(c.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                                if (c.rating > 0) Text("⭐ ${"%.1f".format(c.rating)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        if (c.phone.isNotEmpty()) {
                            Button(onClick = { openWhatsApp(c.phone) }, colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, color = Error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Botones según estado ──────────────────────────
            when (req.status) {

                "pendiente", "en_revision" -> {
                    if (yaMarcoInteres) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⏳", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Solicitud enviada", style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.SemiBold)
                                    Text("Estás a la espera de que el cliente te elija.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showCancelReasonDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancelar solicitud", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        HomefixButton(text = " Me interesa", onClick = { showConfirmInterestDialog = true }, isLoading = actionLoading, color = Primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                        ) {
                            Text(" No me interesa", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                "aceptada" -> {
                    if (req.technicianId == technicianId) {
                        HomefixButton(text = " Trabajo completado", onClick = { showCompletadoDialog = true }, isLoading = actionLoading, color = Success)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showSinContinuarDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.5f))
                        ) {
                            Text("✕ Proceso sin continuar", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                "pendiente_confirmacion" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Warning.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏳", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Esperando confirmación", style = MaterialTheme.typography.titleMedium, color = Warning, fontWeight = FontWeight.SemiBold)
                                Text("El cliente debe confirmar que el trabajo fue completado.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }

                "pendiente_sin_continuar" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏳", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Esperando confirmación", style = MaterialTheme.typography.titleMedium, color = Error, fontWeight = FontWeight.SemiBold)
                                Text("El cliente debe confirmar que el proceso no continuó.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }

                "completada" -> {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(" Servicio completado", style = MaterialTheme.typography.titleMedium, color = Success, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                "sin_continuar" -> {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.08f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(" Proceso no continuado", style = MaterialTheme.typography.titleMedium, color = Error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}