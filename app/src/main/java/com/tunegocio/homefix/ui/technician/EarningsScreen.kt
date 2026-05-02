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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.BorderStroke

@Composable
fun EarningsScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var completedRequests by remember { mutableStateOf(listOf<RequestModel>()) }
    var reviews by remember { mutableStateOf(listOf<ReviewModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var averageRating by remember { mutableStateOf(0f) }

    var seccionReseñasExpandida by remember { mutableStateOf(true) }
    var seccionServiciosExpandida by remember { mutableStateOf(true) }

    var activeRequests by remember { mutableStateOf(listOf<RequestModel>()) }

    LaunchedEffect(uid) {
        // Cargar servicios completados
        db.collection("requests")
            .whereEqualTo("technicianId", uid)
            .whereEqualTo("status", "completada")
            .addSnapshotListener { snapshot, _ ->
                completedRequests = snapshot?.documents?.mapNotNull {
                    it.toObject(RequestModel::class.java)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                isLoading = false
            }

        // Cargar reseñas
        db.collection("reviews")
            .whereEqualTo("technicianId", uid)
            .addSnapshotListener { snapshot, _ ->
                reviews = snapshot?.documents?.mapNotNull {
                    it.toObject(ReviewModel::class.java)
                } ?: emptyList()
                averageRating = if (reviews.isNotEmpty()) {
                    reviews.map { it.stars }.average().toFloat()
                } else 0f
            }


        // Dentro del LaunchedEffect, agrega:
        db.collection("requests")
            .whereEqualTo("technicianId", uid)
            .whereEqualTo("status", "aceptada")
            .addSnapshotListener { snapshot, _ ->
                activeRequests = snapshot?.documents?.mapNotNull {
                    it.toObject(RequestModel::class.java)
                } ?: emptyList()
            }


    }

    // Servicios del mes actual
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    val monthlyRequests = completedRequests.filter { request ->
        val cal = Calendar.getInstance().apply { timeInMillis = request.createdAt }
        cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
    }

    Scaffold(
        bottomBar = {
            TechnicianBottomBar(navController = navController, current = "earnings")
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Mi actividad",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Resumen de tu desempeño",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // Métricas principales
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            value = completedRequests.size.toString(),
                            label = "Total servicios",
                            icon = Icons.Default.CheckCircle,
                            color = Success,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            value = monthlyRequests.size.toString(),
                            label = "Este mes",
                            icon = Icons.Default.CalendarToday,
                            color = Info,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            value = if (averageRating > 0)
                                "${"%.1f".format(averageRating)} ⭐"
                            else "Sin calif.",
                            label = "Calificación",
                            icon = Icons.Default.Star,
                            color = Warning,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            value = reviews.size.toString(),
                            label = "Reseñas",
                            icon = Icons.Default.RateReview,
                            color = Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }




                if (activeRequests.isNotEmpty()) {
                    item {
                        Text("En curso", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    items(activeRequests) { request ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                navController.navigate(Routes.requestDetail(request.requestId))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, Info.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🔧", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(request.serviceType, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text(request.district.ifEmpty { request.address }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Info)
                            }
                        }
                    }
                }





                // Reseñas recientes
                if (reviews.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { seccionReseñasExpandida = !seccionReseñasExpandida },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reseñas recientes",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (seccionReseñasExpandida) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }

                    if (seccionReseñasExpandida) {
                        items(reviews.sortedByDescending { it.createdAt }.take(5)) { review ->
                            ReviewCard(review = review)
                        }
                    }
                }
                // Servicios completados recientes
                if (completedRequests.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { seccionServiciosExpandida = !seccionServiciosExpandida },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Servicios completados",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (seccionServiciosExpandida) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }

                    if (seccionServiciosExpandida) {
                        items(completedRequests.take(10)) { request ->
                            CompletedServiceCard(
                                request = request
                            )

                        }
                    }

                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ReviewCard(review: ReviewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Estrellas
                Row {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= review.stars)
                                Icons.Default.Star
                            else
                                Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (star <= review.stars) Warning else TextHint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Text(
                    text = dateFormat.format(Date(review.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun CompletedServiceCard(
    request: RequestModel
){
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val date = dateFormat.format(Date(request.createdAt))
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Fila principal
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Success.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.serviceType,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Detalle expandible
            if (expandido) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = TextHint.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                if (request.description.isNotEmpty()) {
                    FilaDetalle(
                        icono = Icons.Default.Description,
                        etiqueta = "Descripción",
                        valor = request.description
                    )
                }
                if (request.address.isNotEmpty()) {
                    FilaDetalle(
                        icono = Icons.Default.LocationOn,
                        etiqueta = "Dirección",
                        valor = request.address
                    )
                }
                if (request.district.isNotEmpty()) {
                    FilaDetalle(
                        icono = Icons.Default.Map,
                        etiqueta = "Distrito",
                        valor = request.district
                    )
                }
                if (request.reference.isNotEmpty()) {
                    FilaDetalle(
                        icono = Icons.Default.Info,
                        etiqueta = "Referencia",
                        valor = request.reference
                    )
                }
                if (request.isUrgent) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF6B6B).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "⚡ Urgente",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDetalle(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: String,
    valor: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(15.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
        }
    }
}