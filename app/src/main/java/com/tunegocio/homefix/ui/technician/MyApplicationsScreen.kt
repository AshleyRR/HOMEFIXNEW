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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*

// Pantalla "Mis postulaciones": muestra las solicitudes donde el técnico ya marcó "Me interesa"
// pero el cliente todavía NO lo ha elegido (status sigue en pendiente o en_revision).
// Cuando el cliente elige al técnico, la solicitud pasa a status "aceptada" y desaparece de aquí
// (a partir de ahí se ve en Actividad > "En curso", como ya está resuelto).
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

    LaunchedEffect(uid) {
        db.collection("requests")
            .whereArrayContains("interestedTechnicians", uid)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                val all = snapshot?.documents?.mapNotNull { it.toObject(RequestModel::class.java) } ?: emptyList()
                // Solo las que aún no fueron asignadas a ningún técnico (esperando decisión del cliente)
                applications = all.filter { it.status == "pendiente" || it.status == "en_revision" }
                    .sortedByDescending { it.createdAt }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis postulaciones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        bottomBar = { TechnicianBottomBar(navController = navController, current = "") }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(bgColor).padding(padding)) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                applications.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PendingActions, contentDescription = null, tint = secondaryText, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sin postulaciones activas", style = MaterialTheme.typography.titleMedium, color = textColor)
                            Text("Aquí verás las solicitudes en las que marcaste \"Me interesa\"", style = MaterialTheme.typography.bodyMedium, color = secondaryText)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "${applications.size} postulación${if (applications.size != 1) "es" else ""} en espera",
                                style = MaterialTheme.typography.bodyMedium,
                                color = secondaryText
                            )
                        }
                        items(applications) { request ->
                            NearbyRequestCard(
                                request = request,
                                distance = null,
                                onClick = { navController.navigate(Routes.requestDetail(request.requestId)) },
                                textColor = textColor,
                                secondaryText = secondaryText,
                                cardColor = surfaceColor
                            )
                        }
                    }
                }
            }
        }
    }
}