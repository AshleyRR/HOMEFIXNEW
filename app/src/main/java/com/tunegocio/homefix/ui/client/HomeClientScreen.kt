package com.tunegocio.homefix.ui.client

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*
import com.tunegocio.homefix.viewmodel.NotificationsViewModel

import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

import androidx.compose.ui.platform.LocalContext
import com.tunegocio.homefix.data.local.database.LocalDatabase
@Composable
fun HomeClientScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var userName by remember { mutableStateOf("") }
    var requests by remember { mutableStateOf(listOf<RequestModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var userPhotoUrl by remember { mutableStateOf("") }

    // ViewModel para mostrar badge de notificaciones no leídas
    val notificationsViewModel: NotificationsViewModel = viewModel()
    val noLeidas by notificationsViewModel.noLeidas.collectAsState()

    val context = LocalContext.current
    val localDb = LocalDatabase(context)

    // Cargar datos del usuario
    LaunchedEffect(uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userName = doc.getString("name") ?: ""
                userPhotoUrl = doc.getString("selfieUrl") ?: ""
            }

        // Escuchar solicitudes en tiempo real
        db.collection("requests")
            .whereEqualTo("clientId", uid)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                requests = snapshot?.documents?.mapNotNull {
                    it.toObject(RequestModel::class.java)
                } ?: emptyList()

                // Guardar solicitudes en SQLite local
                requests.forEach { request ->
                    localDb.guardarSolicitud(
                        requestId = request.requestId,
                        clientId = request.clientId,
                        technicianId = request.technicianId,
                        tipoServicio = request.serviceType,
                        descripcion = request.description,
                        direccion = request.address,
                        referencia = request.reference,
                        distrito = request.district,
                        estado = request.status,
                        esUrgente = request.isUrgent,
                        imagenUrl = request.imageUrls.firstOrNull() ?: "",
                        lat = request.lat,
                        lng = request.lng,
                        creadoEn = request.createdAt,
                        actualizadoEn = request.updatedAt
                    )
                }
            }
    }

    Scaffold(
        bottomBar = { ClientBottomBar(navController = navController, current = "home") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hola, ${userName.split(" ").firstOrNull() ?: ""}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "¿Qué necesitas reparar hoy?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    // Íconos de notificaciones y perfil en el header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Ícono notificaciones con badge de no leídas
                        BadgedBox(
                            badge = {
                                if (noLeidas > 0) {
                                    Badge { Text(text = noLeidas.toString()) }
                                }
                            }
                        ) {
                            IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notificaciones",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                            if (userPhotoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = "Perfil",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(18.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = "Perfil",
                                    tint = Primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Botón nueva solicitud
                Button(
                    onClick = { navController.navigate(Routes.NEW_REQUEST) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nueva solicitud",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Botón ver técnicos
                OutlinedButton(
                    onClick = { navController.navigate(Routes.TECHNICIAN_LIST) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Secondary)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver técnicos disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mis solicitudes activas",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            } else if (requests.isEmpty()) {
                item { EmptyRequestsCard() }
            } else {
                items(requests.filter {
                    it.status != "completada" && it.status != "cancelada"
                }) { request ->
                    RequestStatusCard(
                        request = request,
                        onClick = {
                            navController.navigate(Routes.requestTracking(request.requestId))
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun EmptyRequestsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🔍", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sin solicitudes activas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Crea una nueva solicitud para encontrar un técnico",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun RequestStatusCard(request: RequestModel, onClick: () -> Unit) {
    val statusColor = when (request.status) {
        "pendiente" -> Warning
        "en_revision" -> Info
        "aceptada" -> Success
        "en_camino" -> Secondary
        else -> TextSecondary
    }
    val statusLabel = when (request.status) {
        "pendiente" -> "Pendiente"
        "en_revision" -> "En revisión"
        "aceptada" -> "Aceptada"
        "en_camino" -> "En camino"
        else -> request.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.serviceType,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = request.description.take(60) + if (request.description.length > 60) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (request.isUrgent) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚡ Urgente",
                        style = MaterialTheme.typography.labelSmall,
                        color = Error
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ClientBottomBar(navController: NavController, current: String) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = current == "home",
            onClick = { navController.navigate(Routes.HOME_CLIENT) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = current == "technicians",
            onClick = { navController.navigate(Routes.TECHNICIAN_LIST) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Técnicos") },
            label = { Text("Técnicos") }
        )
        NavigationBarItem(
            selected = current == "history",
            onClick = { navController.navigate(Routes.HISTORY) },
            icon = { Icon(Icons.Default.List, contentDescription = "Historial") },
            label = { Text("Historial") }
        )
        NavigationBarItem(
            selected = current == "profile",
            onClick = { navController.navigate(Routes.PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
            label = { Text("Perfil") }
        )
    }
}