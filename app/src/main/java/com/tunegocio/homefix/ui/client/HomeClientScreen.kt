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
import androidx.compose.ui.res.stringResource
import com.tunegocio.homefix.data.local.database.LocalDatabase
import com.tunegocio.homefix.R

// Traduce la etiqueta visible del tipo de servicio. El valor guardado en
// Firebase se mantiene igual (en español) para no afectar filtros ni consultas.
@Composable
private fun clientServiceLabel(serviceType: String): String = when (serviceType) {
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

// Traduce la etiqueta visible del estado de la solicitud. El código interno
// del estado (usado en comparaciones y lógica) no cambia.
@Composable
private fun clientStatusLabel(status: String): String = when (status) {
    "pendiente" -> stringResource(R.string.status_pending)
    "en_revision" -> stringResource(R.string.status_under_review)
    "aceptada" -> stringResource(R.string.status_accepted)
    "en_camino" -> stringResource(R.string.status_on_the_way)
    "completada" -> stringResource(R.string.status_completed)
    "cancelada" -> stringResource(R.string.status_canceled)
    "sin_continuar" -> stringResource(R.string.status_not_continued)
    else -> status
}

// Pantalla principal (Home) del cliente: saludo, botón de nueva solicitud
// y lista de solicitudes activas en tiempo real
@Composable
fun HomeClientScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var userName by remember { mutableStateOf("") }
    var requests by remember { mutableStateOf(listOf<RequestModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var userPhotoUrl by remember { mutableStateOf("") }

    // ViewModel para el badge de notificaciones no leídas
    val notificationsViewModel: NotificationsViewModel = viewModel()
    val noLeidas by notificationsViewModel.noLeidas.collectAsState()

    val context = LocalContext.current
    val localDb = LocalDatabase(context)

    // Carga los datos del usuario y escucha sus solicitudes en tiempo real
    LaunchedEffect(uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userName = doc.getString("name") ?: ""
                userPhotoUrl = doc.getString("selfieUrl") ?: ""
            }

        db.collection("requests")
            .whereEqualTo("clientId", uid)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                requests = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.toObject(RequestModel::class.java)
                    }
                    ?.sortedByDescending { request ->
                        request.createdAt
                    }
                    ?: emptyList()

                // Guarda cada solicitud en la base de datos local (SQLite)
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
                // Encabezado: saludo + íconos de notificaciones y perfil
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(
                                R.string.home_client_greeting,
                                userName.split(" ").firstOrNull() ?: ""
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.home_client_question),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Notificaciones con badge de mensajes no leídos
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
                                    contentDescription = stringResource(R.string.client_notifications),
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                            if (userPhotoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = stringResource(R.string.client_profile),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(18.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = stringResource(R.string.client_profile),
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
                // Botón para crear una nueva solicitud
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
                        text = stringResource(R.string.home_client_new_request),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_client_active_requests),
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
                // Excluye completadas y canceladas de la lista de activas
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

// Tarjeta que se muestra cuando el cliente no tiene solicitudes activas
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
                text = stringResource(R.string.home_client_no_active_requests),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.home_client_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

// Tarjeta individual de una solicitud: servicio, descripción, urgencia y estado
@Composable
fun RequestStatusCard(request: RequestModel, onClick: () -> Unit) {
    val statusColor = when (request.status) {
        "pendiente" -> Warning
        "en_revision" -> Info
        "aceptada" -> Success
        "en_camino" -> Secondary
        else -> TextSecondary
    }
    val statusLabel = clientStatusLabel(request.status)

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
                    text = clientServiceLabel(request.serviceType),
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
                        text = "⚡ ${stringResource(R.string.client_urgent)}",
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

// Barra de navegación inferior del cliente (home, técnicos, historial, perfil)
@Composable
fun ClientBottomBar(navController: NavController, current: String) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = current == "home",
            onClick = { navController.navigate(Routes.HOME_CLIENT) },
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = stringResource(R.string.client_nav_home)
                )
            },
            label = { Text(stringResource(R.string.client_nav_home)) }
        )
        NavigationBarItem(
            selected = current == "technicians",
            onClick = { navController.navigate(Routes.TECHNICIAN_LIST) },
            icon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(R.string.client_nav_technicians)
                )
            },
            label = { Text(stringResource(R.string.client_nav_technicians)) }
        )
        NavigationBarItem(
            selected = current == "history",
            onClick = { navController.navigate(Routes.HISTORY) },
            icon = {
                Icon(
                    Icons.Default.List,
                    contentDescription = stringResource(R.string.client_nav_history)
                )
            },
            label = { Text(stringResource(R.string.client_nav_history)) }
        )
        NavigationBarItem(
            selected = current == "profile",
            onClick = { navController.navigate(Routes.PROFILE) },
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = stringResource(R.string.client_nav_profile)
                )
            },
            label = { Text(stringResource(R.string.client_nav_profile)) }
        )
    }
}