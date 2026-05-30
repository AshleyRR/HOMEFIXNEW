package com.tunegocio.homefix.ui.shared

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import com.tunegocio.homefix.ui.client.ClientBottomBar
import com.tunegocio.homefix.ui.technician.TechnicianBottomBar

import androidx.compose.ui.platform.LocalContext
import com.tunegocio.homefix.data.local.database.LocalDatabase

@Composable
fun HistoryScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var requests by remember { mutableStateOf(listOf<RequestModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var userRole by remember { mutableStateOf("client") }

    val context = LocalContext.current
    val localDb = LocalDatabase(context)

    val filters = listOf("Todos", "Completadas", "Canceladas")

    LaunchedEffect(uid) {
        // Obtener rol
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userRole = doc.getString("role") ?: "client"
            }

        // Campo a filtrar según rol
        val field = if (userRole == "client") "clientId" else "technicianId"

        db.collection("requests")
            .whereEqualTo(field, uid)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                requests = snapshot?.documents?.mapNotNull {
                    it.toObject(RequestModel::class.java)
                }?.filter {
                    it.status == "completada" || it.status == "cancelada" || it.status == "sin_continuar"
                }?.sortedByDescending { it.createdAt } ?: emptyList()


                // Guardar historial en SQLite local
                requests.forEach { request ->
                    localDb.guardarHistorial(
                        requestId = request.requestId,
                        tipoServicio = request.serviceType,
                        descripcion = request.description,
                        distrito = request.district,
                        estadoFinal = request.status,
                        clienteNombre = "",
                        tecnicoNombre = "",
                        calificacionDada = 0,
                        comentarioDado = "",
                        creadoEn = request.createdAt,
                        completadoEn = request.updatedAt
                    )
                }

            }
    }

    val filteredRequests = when (selectedFilter) {
        "Completadas" -> requests.filter { it.status == "completada" }
        "Canceladas" -> requests.filter { it.status == "cancelada" }
        else -> requests
    }

    Scaffold(
        bottomBar = {
            if (userRole == "client") {
                ClientBottomBar(navController = navController, current = "history")
            } else {
                TechnicianBottomBar(navController = navController, current = "history")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Historial",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Todos tus servicios pasados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Filtros
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    filter,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (filteredRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📋",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Sin historial aún",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Aquí aparecerán tus servicios completados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "${filteredRequests.size} servicio${if (filteredRequests.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    items(filteredRequests) { request ->
                        HistoryCard(
                            request = request,
                            userRole = userRole,
                            onClick = {
                                if (userRole == "client") {
                                    navController.navigate(
                                        Routes.requestTracking(request.requestId)
                                    )
                                } else {
                                    navController.navigate(
                                        Routes.requestDetail(request.requestId)
                                    )
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    request: RequestModel,
    userRole: String,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val date = dateFormat.format(Date(request.createdAt))



    val statusColor = when (request.status) {
        "completada"    -> Success
        "sin_continuar" -> Warning
        else            -> Error
    }
    val statusLabel = when (request.status) {
        "completada"    -> "Completada"
        "sin_continuar" -> "No continuada"
        else            -> "Cancelada"
    }
    val statusEmoji = when (request.status) {
        "completada"    -> ""
        "sin_continuar" -> ""
        else            -> ""
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.serviceType,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = request.description.take(60) +
                                if (request.description.length > 60) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$statusEmoji $statusLabel",
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ver detalle",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}