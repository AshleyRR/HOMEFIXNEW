package com.tunegocio.homefix.ui.technician

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.LocationUtils
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*
import com.tunegocio.homefix.viewmodel.NotificationsViewModel

@SuppressLint("MissingPermission")
@Composable
fun HomeTechnicianScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val uid = auth.currentUser?.uid ?: ""

    var userName by remember { mutableStateOf("") }
    var userDistrict by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) } // false es el valor correcto por defecto
    var techLat by remember { mutableStateOf(0.0) }
    var techLng by remember { mutableStateOf(0.0) }
    var hasGps by remember { mutableStateOf(false) }
    var allRequests by remember { mutableStateOf(listOf<RequestModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDistrictFilter by remember { mutableStateOf("Todos") }
    var techSpecialties by remember { mutableStateOf(listOf<String>()) }

    // ViewModel para badge de notificaciones no leídas
    val notificationsViewModel: NotificationsViewModel = viewModel()
    val noLeidas by notificationsViewModel.noLeidas.collectAsState()

    // Launcher para pedir permiso de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            val fusedLocation = LocationServices.getFusedLocationProviderClient(context)
            fusedLocation.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    techLat = location.latitude
                    techLng = location.longitude
                    hasGps = true
                    // Guardar ubicación en Firestore
                    db.collection("users").document(uid)
                        .update(mapOf("lat" to techLat, "lng" to techLng))
                } else {
                    // Usar coordenadas del distrito registrado como fallback
                    val coords = LocationUtils.districtCoordinates[userDistrict]
                    if (coords != null) {
                        techLat = coords.first
                        techLng = coords.second
                    }
                }
            }
        }
    }

    LaunchedEffect(uid) {
        // Cargar datos del técnico
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userName = doc.getString("name") ?: ""
                userDistrict = doc.getString("district") ?: ""
                isActive = doc.getBoolean("isActive") ?: false
                techLat = doc.getDouble("lat") ?: 0.0
                techLng = doc.getDouble("lng") ?: 0.0
                hasGps = techLat != 0.0 && techLng != 0.0

                // Cargar especialidades del técnico
                @Suppress("UNCHECKED_CAST")
                techSpecialties = (doc.get("specialties") as? List<String>) ?: emptyList()

                // Si está activo, actualizar ubicación al abrir
                if (isActive) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                // Si no tiene GPS, usar coordenadas del distrito como fallback
                if (!hasGps) {
                    val coords = LocationUtils.districtCoordinates[userDistrict]
                    if (coords != null) {
                        techLat = coords.first
                        techLng = coords.second
                    }
                }
            }

        // Escuchar solicitudes pendientes en tiempo real
        db.collection("requests")
            .whereEqualTo("status", "pendiente")
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                allRequests = snapshot?.documents?.mapNotNull {
                    it.toObject(RequestModel::class.java)
                } ?: emptyList()
            }
    }

    fun toggleActive(newValue: Boolean) {
        isActive = newValue
        val updates = mutableMapOf<String, Any>("isActive" to newValue)
        if (newValue) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        db.collection("users").document(uid).update(updates)
    }

    fun refreshLocation() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Filtrar por especialidad del técnico (independencia de especialidad)
    val specialtyFilteredRequests = if (techSpecialties.isEmpty()) {
        allRequests // Sin especialidades registradas: ve todas
    } else {
        allRequests.filter { request ->
            techSpecialties.any { specialty ->
                specialty.equals(request.serviceType, ignoreCase = true)
            }
        }
    }

    // Calcular distancia sobre las solicitudes ya filtradas por especialidad
    val requestsWithDistance = specialtyFilteredRequests.map { request ->
        val distance = if (techLat != 0.0 && techLng != 0.0 &&
            request.lat != 0.0 && request.lng != 0.0
        ) {
            LocationUtils.haversineDistance(techLat, techLng, request.lat, request.lng)
        } else null
        Pair(request, distance)
    }

    // Filtrar por distrito si hay filtro activo
    val filteredRequests = if (selectedDistrictFilter == "Todos") {
        requestsWithDistance
    } else {
        requestsWithDistance.filter { (request, _) ->
            request.district == selectedDistrictFilter
        }
    }

    // Separar en cercanas (≤10 km) y otras
    val nearbyRequests = filteredRequests
        .filter { (_, distance) -> distance != null && distance <= 10.0 }
        .sortedBy { (_, distance) -> distance }

    val otherRequests = filteredRequests
        .filter { (_, distance) -> distance == null || distance > 10.0 }
        .sortedBy { (_, distance) -> distance ?: Double.MAX_VALUE }

    // Distritos únicos de las solicitudes para el filtro
    val availableDistricts = listOf("Todos") +
            allRequests.map { it.district }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()

    Scaffold(
        bottomBar = {
            TechnicianBottomBar(navController = navController, current = "home")
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hola, ${userName.split(" ").firstOrNull() ?: ""} ",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isActive) "Estás disponible" else "Estás inactivo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isActive) Success else TextSecondary
                            )
                            if (userDistrict.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = userDistrict,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        // Íconos de notificaciones y perfil
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        tint = Primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
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

            // Toggle disponibilidad
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive)
                            Success.copy(alpha = 0.1f)
                        else SurfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isActive) "Disponible" else "No disponible",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isActive) Success else TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isActive)
                                        "Recibes solicitudes de clientes"
                                    else
                                        "Actívate para recibir solicitudes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = isActive,
                                onCheckedChange = { toggleActive(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Success
                                )
                            )
                        }

                        // Banner GPS + botón refrescar
                        if (isActive) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = CardBorder)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(8.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (hasGps) Success else Warning
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasGps)
                                            "GPS activo — solicitudes ordenadas por distancia"
                                        else
                                            "Sin GPS — usando distrito: $userDistrict",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasGps) Success else Warning
                                    )
                                }
                                IconButton(
                                    onClick = { refreshLocation() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Actualizar ubicación",
                                        tint = Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isActive) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "😴",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Estás inactivo",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Activa tu disponibilidad para ver solicitudes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {

                // Filtro por distrito
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Filtrar por distrito",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableDistricts) { district ->
                            FilterChip(
                                selected = selectedDistrictFilter == district,
                                onClick = { selectedDistrictFilter = district },
                                label = {
                                    Text(
                                        district,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
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
                } else {

                    // Sección: Cerca de ti
                    if (nearbyRequests.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Success,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cerca de ti (${nearbyRequests.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        items(nearbyRequests) { (request, distance) ->
                            NearbyRequestCard(
                                request = request,
                                distance = distance?.let { LocationUtils.formatDistance(it) },
                                onClick = {
                                    navController.navigate(Routes.requestDetail(request.requestId))
                                },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }

                    // Sección: Otros distritos
                    if (otherRequests.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Otros distritos (${otherRequests.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        items(otherRequests) { (request, distance) ->
                            NearbyRequestCard(
                                request = request,
                                distance = distance?.let { LocationUtils.formatDistance(it) },
                                onClick = {
                                    navController.navigate(Routes.requestDetail(request.requestId))
                                },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }

                    // Sin solicitudes
                    if (nearbyRequests.isEmpty() && otherRequests.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🎉",
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Sin solicitudes por ahora",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Te notificaremos cuando llegue una nueva",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun NearbyRequestCard(
    request: RequestModel,
    distance: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
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
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = request.description.take(80) +
                                if (request.description.length > 80) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                if (request.isUrgent) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Error.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "⚡ Urgente",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distrito
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (request.district.isNotEmpty())
                            request.district
                        else
                            request.address.ifEmpty { "Ubicación no especificada" },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // Distancia
                if (distance != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = distance,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TechnicianBottomBar(navController: NavController, current: String) {
    NavigationBar(containerColor = Surface) {
        NavigationBarItem(
            selected = current == "home",
            onClick = { navController.navigate(Routes.HOME_TECHNICIAN) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Solicitudes") }
        )
        NavigationBarItem(
            selected = current == "earnings",
            onClick = { navController.navigate(Routes.EARNINGS) },
            icon = { Icon(Icons.Default.Star, contentDescription = "Actividad") },
            label = { Text("Actividad") }
        )
        NavigationBarItem(
            selected = current == "profile",
            onClick = { navController.navigate(Routes.PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
            label = { Text("Perfil") }
        )
    }
}