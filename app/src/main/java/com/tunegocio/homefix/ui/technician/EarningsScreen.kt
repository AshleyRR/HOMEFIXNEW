package com.tunegocio.homefix.ui.technician

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.data.model.ReviewModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*


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
@Composable
fun EarningsScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    var completedRequests by remember { mutableStateOf(listOf<RequestModel>()) }
    var sinContinuarRequests by remember { mutableStateOf(listOf<RequestModel>()) }
    var reviews by remember { mutableStateOf(listOf<ReviewModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var averageRating by remember { mutableStateOf(0f) }
    var userName by remember { mutableStateOf("") }
    var clienteNombresMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(uid) {
        // Nombre del técnico
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userName = doc.getString("name") ?: ""
            }

        // Servicios completados
        db.collection("requests")
            .whereEqualTo("technicianId", uid)
            .whereEqualTo("status", "completada")
            .addSnapshotListener { snapshot, _ ->
                completedRequests = snapshot?.documents?.mapNotNull {
                    it.toObject(RequestModel::class.java)
                }?.sortedByDescending { it.updatedAt } ?: emptyList()
                isLoading = false

                // Cargar nombres de clientes en una sola consulta
                val clientIds = completedRequests.map { it.clientId }.filter { it.isNotEmpty() }.distinct()
                if (clientIds.isNotEmpty()) {
                    clientIds.chunked(10).forEach { chunk ->
                        db.collection("users")
                            .whereIn("uid", chunk)
                            .get()
                            .addOnSuccessListener { clientSnap ->
                                val nuevos = clientSnap.documents.associate { doc ->
                                    (doc.getString("uid") ?: "") to (doc.getString("name") ?: "Cliente")
                                }
                                clienteNombresMap = clienteNombresMap + nuevos
                            }
                    }
                }
            }

        // Servicios sin continuar
        db.collection("requests")
            .whereEqualTo("technicianId", uid)
            .whereEqualTo("status", "sin_continuar")
            .addSnapshotListener { snapshot, _ ->
                sinContinuarRequests = snapshot?.documents?.mapNotNull {
                    it.toObject(RequestModel::class.java)
                }?.sortedByDescending { it.updatedAt } ?: emptyList()

                // Nombres de clientes sin continuar
                val clientIds = sinContinuarRequests.map { it.clientId }.filter { it.isNotEmpty() }.distinct()
                if (clientIds.isNotEmpty()) {
                    clientIds.chunked(10).forEach { chunk ->
                        db.collection("users")
                            .whereIn("uid", chunk)
                            .get()
                            .addOnSuccessListener { clientSnap ->
                                val nuevos = clientSnap.documents.associate { doc ->
                                    (doc.getString("uid") ?: "") to (doc.getString("name") ?: "Cliente")
                                }
                                clienteNombresMap = clienteNombresMap + nuevos
                            }
                    }
                }
            }

        // Reseñas
        db.collection("reviews")
            .whereEqualTo("technicianId", uid)
            .addSnapshotListener { snapshot, _ ->
                reviews = snapshot?.documents?.mapNotNull {
                    it.toObject(ReviewModel::class.java)
                } ?: emptyList()
                averageRating = if (reviews.isNotEmpty())
                    reviews.map { it.stars }.average().toFloat()
                else 0f
            }
    }

    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    val monthlyRequests = completedRequests.filter { request ->
        val cal = Calendar.getInstance().apply { timeInMillis = request.updatedAt }
        cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
    }

    Scaffold(
        bottomBar = { TechnicianBottomBar(navController = navController, current = "earnings") }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Mi actividad",
                                style = MaterialTheme.typography.headlineMedium,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )

                        }
                        // Calificación promedio destacada
                        if (averageRating > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Warning.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Warning, modifier = Modifier.size(18.dp))
                                    Text(
                                        "${"%.1f".format(averageRating)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Warning,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Métricas
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                value = completedRequests.size.toString(),
                                label = "Completados",
                                icon = Icons.Default.CheckCircle,
                                color = Success,
                                modifier = Modifier.weight(1f),
                                textColor = textColor,
                                subtitleColor = secondaryText,
                                cardColor = surfaceColor
                            )
                            MetricCard(
                                value = monthlyRequests.size.toString(),
                                label = "Este mes",
                                icon = Icons.Default.CalendarToday,
                                color = Info,
                                modifier = Modifier.weight(1f),
                                textColor = textColor,
                                subtitleColor = secondaryText,
                                cardColor = surfaceColor
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                value = sinContinuarRequests.size.toString(),
                                label = "Sin continuar",
                                icon = Icons.Default.Cancel,
                                color = Error,
                                modifier = Modifier.weight(1f),
                                textColor = textColor,
                                subtitleColor = secondaryText,
                                cardColor = surfaceColor
                            )
                            MetricCard(
                                value = reviews.size.toString(),
                                label = "Reseñas",
                                icon = Icons.Default.RateReview,
                                color = Warning,
                                modifier = Modifier.weight(1f),
                                textColor = textColor,
                                subtitleColor = secondaryText,
                                cardColor = surfaceColor
                            )
                        }
                    }
                }

                // Sección servicios completados
                if (completedRequests.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Servicios completados",
                                style = MaterialTheme.typography.titleLarge,
                                color = textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Success.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "${completedRequests.size}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Success,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    items(completedRequests) { request ->
                        ActividadServiceCard(
                            request = request,
                            clienteNombre = clienteNombresMap[request.clientId] ?: "",
                            esCompletado = true,
                            textColor = textColor,
                            secondaryText = secondaryText,
                            surfaceColor = surfaceColor,
                            onClick = { navController.navigate(Routes.requestDetail(request.requestId)) }
                        )
                    }
                }

                // Sección sin continuar
                if (sinContinuarRequests.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Sin continuar",
                                style = MaterialTheme.typography.titleLarge,
                                color = textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Error.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "${sinContinuarRequests.size}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    items(sinContinuarRequests) { request ->
                        ActividadServiceCard(
                            request = request,
                            clienteNombre = clienteNombresMap[request.clientId] ?: "",
                            esCompletado = false,
                            textColor = textColor,
                            secondaryText = secondaryText,
                            surfaceColor = surfaceColor,
                            onClick = { navController.navigate(Routes.requestDetail(request.requestId)) }
                        )
                    }
                }

                // Estado vacío
                if (completedRequests.isEmpty() && sinContinuarRequests.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
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
                                            Icons.Default.WorkHistory,
                                            contentDescription = null,
                                            tint = secondaryText,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Text(
                                    "Sin actividad aún",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Aquí verás tus servicios completados",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = secondaryText
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

// Card unificado para completados y sin continuar
@Composable
fun ActividadServiceCard(
    request: RequestModel,
    clienteNombre: String,
    esCompletado: Boolean,
    textColor: androidx.compose.ui.graphics.Color,
    secondaryText: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val color = if (esCompletado) Success else Error
    val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        .format(Date(request.updatedAt))

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
            // Ícono de estado
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getServiceTypeIcon(request.serviceType),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Tipo de servicio
                Text(
                    request.serviceType,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )

                // Nombre del cliente
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

                // Distrito
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
                // Fecha
                Text(
                    fecha,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Badge urgente
                if (request.isUrgent) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Error.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Error, modifier = Modifier.size(10.dp))
                            Text("Urgente", style = MaterialTheme.typography.labelSmall, color = Error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
    subtitleColor: androidx.compose.ui.graphics.Color = TextSecondary,
    cardColor: androidx.compose.ui.graphics.Color = CardBackground
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = textColor, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = subtitleColor)
        }
    }
}