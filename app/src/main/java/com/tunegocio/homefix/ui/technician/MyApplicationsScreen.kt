package com.tunegocio.homefix.ui.technician

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.R
import com.tunegocio.homefix.ui.theme.*

// Traduce la etiqueta visible del tipo de servicio; el valor interno
// (usado para filtrar/guardar en Firebase) nunca se modifica
@Composable
private fun applicationsServiceLabel(serviceType: String): String {
    return when (serviceType) {
        "Electricidad" -> stringResource(R.string.technician_service_electricity)
        "Gasfitería" -> stringResource(R.string.technician_service_plumbing)
        "Pintura" -> stringResource(R.string.technician_service_painting)
        "Carpintería" -> stringResource(R.string.technician_service_carpentry)
        "Vidriería" -> stringResource(R.string.technician_service_glasswork)
        "Jardinería" -> stringResource(R.string.technician_service_gardening)
        "Cerrajería" -> stringResource(R.string.technician_service_locksmith)
        "Albañilería" -> stringResource(R.string.technician_service_masonry)
        "Muebles a medida" -> stringResource(R.string.technician_service_custom_furniture)
        "Lavado de tapizados" -> stringResource(R.string.technician_service_upholstery_cleaning)
        "Mudanzas" -> stringResource(R.string.technician_service_moving)
        else -> serviceType
    }
}

// Mapea el tipo de servicio a su ícono; si no hay coincidencia usa uno genérico
private fun getServiceTypeIcon(serviceType: String): ImageVector {
    return when (serviceType) {
        "Electricidad" -> Icons.Default.ElectricalServices
        "Gasfitería" -> Icons.Default.Plumbing
        "Pintura" -> Icons.Default.Palette
        "Carpintería" -> Icons.Default.Carpenter
        "Vidriería" -> Icons.Default.Window
        "Jardinería" -> Icons.Default.Grass
        "Cerrajería" -> Icons.Default.Lock
        "Albañilería" -> Icons.Default.DomainAdd
        "Muebles a medida" -> Icons.Default.Weekend
        "Lavado de tapizados" -> Icons.Default.CleaningServices
        "Mudanzas" -> Icons.Default.LocalShipping
        else -> Icons.Default.Build
    }
}

// Muestra al técnico logueado las postulaciones activas (pendientes/en revisión)
// a solicitudes donde aparece dentro de "interestedTechnicians"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    var applications by remember { mutableStateOf(listOf<RequestModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var clienteNombresMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Nombre por defecto si el cliente no tiene "name" en Firestore
    val defaultClientName = stringResource(R.string.technician_default_client)

    LaunchedEffect(uid) {
        // Escucha en tiempo real las solicitudes donde el técnico está interesado
        db.collection("requests")
            .whereArrayContains("interestedTechnicians", uid)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                val all = snapshot?.documents?.mapNotNull {
                    it.toObject(RequestModel::class.java)
                } ?: emptyList()
                // Solo se muestran las que siguen activas (pendiente o en revisión)
                applications = all
                    .filter { it.status == "pendiente" || it.status == "en_revision" }
                    .sortedByDescending { it.createdAt }

                // Carga los nombres de clientes en lotes de 10 (límite de whereIn)
                val clientIds = applications.map { it.clientId }.filter { it.isNotEmpty() }.distinct()
                if (clientIds.isNotEmpty()) {
                    clientIds.chunked(10).forEach { chunk ->
                        db.collection("users")
                            .whereIn("uid", chunk)
                            .get()
                            .addOnSuccessListener { clientSnap ->
                                val nuevos = clientSnap.documents.associate { doc ->
                                    (doc.getString("uid") ?: "") to (doc.getString("name") ?: defaultClientName)
                                }
                                clienteNombresMap = clienteNombresMap + nuevos
                            }
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.technician_applications_title), fontWeight = FontWeight.Bold, color = textColor)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.technician_back), tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        bottomBar = { TechnicianBottomBar(navController = navController, current = "") }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                applications.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = RoundedCornerShape(36.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PendingActions,
                                        contentDescription = null,
                                        tint = secondaryText,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Text(
                                stringResource(R.string.technician_applications_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                stringResource(R.string.technician_applications_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = secondaryText
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                pluralStringResource(
                                    R.plurals.technician_applications_waiting_count,
                                    applications.size,
                                    applications.size
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = secondaryText
                            )
                        }
                        items(applications) { request ->
                            PostulacionCard(
                                request = request,
                                clienteNombre = clienteNombresMap[request.clientId] ?: "",
                                textColor = textColor,
                                secondaryText = secondaryText,
                                surfaceColor = surfaceColor,
                                onClick = {
                                    navController.navigate(Routes.requestDetail(request.requestId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Tarjeta de una postulación: servicio, cliente, distrito, fecha, estado y urgencia
@Composable
fun PostulacionCard(
    request: RequestModel,
    clienteNombre: String,
    textColor: androidx.compose.ui.graphics.Color,
    secondaryText: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val statusColor = if (request.status == "en_revision") Info else Warning
    val statusLabel = if (request.status == "en_revision") {
        stringResource(R.string.technician_status_under_review)
    } else {
        stringResource(R.string.technician_status_pending)
    }

    val serviceLabel = applicationsServiceLabel(request.serviceType)
    val fecha = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        .format(java.util.Date(request.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono según la especialidad del servicio
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = Primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getServiceTypeIcon(request.serviceType),
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    serviceLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
                if (clienteNombre.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            clienteNombre,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText
                        )
                    }
                }
                if (request.district.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            request.district,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    fecha,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Chip de estado (pendiente / en revisión)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Chip adicional de "urgente"
                if (request.isUrgent) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Error.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                stringResource(R.string.technician_urgent),
                                style = MaterialTheme.typography.labelSmall,
                                color = Error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}