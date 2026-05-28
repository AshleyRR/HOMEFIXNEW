package com.tunegocio.homefix.ui.client

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.ALL_SPECIALTIES
import com.tunegocio.homefix.data.CloudinaryUploader
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.components.HomefixTextField
import com.tunegocio.homefix.ui.components.MapaUbicacion
import com.tunegocio.homefix.ui.components.estaDentroDeLima
import com.tunegocio.homefix.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.util.*

// Ícono por especialidad
private fun iconoPorEspecialidad(especialidad: String): String {
    return when (especialidad) {
        "Electricidad"        -> "⚡"
        "Gasfitería"          -> "🔧"
        "Pintura"             -> "🎨"
        "Carpintería"         -> "🪚"
        "Vidriería"           -> "🪟"
        "Jardinería"          -> "🌿"
        "Cerrajería"          -> "🔑"
        "Albañilería"         -> "🧱"
        "Muebles a medida"    -> "🛋️"
        "Lavado de tapizados" -> "🧹"
        "Mudanzas"            -> "📦"
        else                  -> "🔨"
    }
}

@SuppressLint("MissingPermission")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NewRequestScreen(navController: NavController) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

    // Repositorio para crear notificaciones a técnicos
    val notificationsRepo = remember { com.tunegocio.homefix.data.NotificationsRepository() }

    // Detecta si la pantalla es ancha — diseño responsive
    val configuracion = LocalConfiguration.current
    val pantallaAncha = configuracion.screenWidthDp >= 360

    var serviceType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }
    var lat by remember { mutableStateOf(-12.0464) }
    var lng by remember { mutableStateOf(-77.0428) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var locationLoaded by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var clientDistrict by remember { mutableStateOf("") }
    var dropdownExpandido by remember { mutableStateOf(false) }

    // Estado del buscador con sugerencias
    var sugerencias by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var mostrarSugerencias by remember { mutableStateOf(false) }
    var jobBusqueda by remember { mutableStateOf<Job?>(null) }

    val serviceTypes = ALL_SPECIALTIES

    // Archivo temporal para la foto de cámara
    val photoFile = remember { File(context.cacheDir, "photo_${UUID.randomUUID()}.jpg") }
    val photoUriForCamera = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) photoUri = photoUriForCamera }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { photoUri = it } }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) cameraLauncher.launch(photoUriForCamera) }

    // Convierte coordenadas a dirección legible con Geocoder
    fun actualizarDireccion(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(context, Locale("es", "PE"))
            @Suppress("DEPRECATION")
            val resultados = geocoder.getFromLocation(latitude, longitude, 1)
            if (!resultados.isNullOrEmpty()) {
                val r = resultados[0]
                address = r.getAddressLine(0)
                    ?: "Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}"
                clientDistrict = r.subLocality ?: r.locality ?: clientDistrict
            }
        } catch (e: Exception) {
            address = "Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}"
        }
    }

    // Busca sugerencias mientras el usuario escribe — espera 400ms (debounce)
    fun buscarSugerencias(query: String) {
        jobBusqueda?.cancel()
        if (query.length < 3) {
            sugerencias = emptyList()
            mostrarSugerencias = false
            return
        }
        jobBusqueda = scope.launch {
            delay(400)
            try {
                val textoCodificado = URLEncoder.encode("$query, Lima, Peru", "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?q=$textoCodificado&format=json&limit=8&countrycodes=pe&accept-language=es"
                val respuesta = withContext(Dispatchers.IO) {
                    URL(url).openConnection().apply {
                        setRequestProperty("User-Agent", "HomeFix-App/1.0")
                        connectTimeout = 5000
                        readTimeout = 5000
                    }.getInputStream().bufferedReader().readText()
                }
                val json = JSONArray(respuesta)
                val resultadosNominatim = mutableListOf<android.location.Address>()
                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    val latItem = item.getDouble("lat")
                    val lngItem = item.getDouble("lon")
                    // Solo resultados dentro de Lima
                    if (estaDentroDeLima(latItem, lngItem)) {
                        val direccion = android.location.Address(Locale("es", "PE")).apply {
                            latitude = latItem
                            longitude = lngItem
                            featureName = item.optString("display_name").split(",").firstOrNull()?.trim() ?: ""
                            setAddressLine(0, item.optString("display_name"))
                        }
                        resultadosNominatim.add(direccion)
                    }
                }
                sugerencias = resultadosNominatim
                mostrarSugerencias = sugerencias.isNotEmpty()
            } catch (e: Exception) {
                // Fallback: Geocoder si Nominatim falla
                try {
                    val geocoder = Geocoder(context, Locale("es", "PE"))
                    @Suppress("DEPRECATION")
                    val resultados = geocoder.getFromLocationName("$query, Lima, Peru", 5)
                    sugerencias = resultados?.filter { estaDentroDeLima(it.latitude, it.longitude) } ?: emptyList()
                    mostrarSugerencias = sugerencias.isNotEmpty()
                } catch (e2: Exception) {
                    sugerencias = emptyList()
                    mostrarSugerencias = false
                }
            }
        }
    }

    // Cuando el usuario selecciona una sugerencia del buscador
    fun seleccionarSugerencia(resultado: android.location.Address) {
        lat = resultado.latitude
        lng = resultado.longitude
        busqueda = resultado.getAddressLine(0) ?: busqueda
        address = resultado.getAddressLine(0) ?: ""
        clientDistrict = resultado.subLocality ?: resultado.locality ?: clientDistrict
        locationLoaded = true
        errorMessage = ""
        mostrarSugerencias = false
        sugerencias = emptyList()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            val fusedLocation = LocationServices.getFusedLocationProviderClient(context)
            // LocationCallback — más confiable que lastLocation en todos los dispositivos
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                1000L
            )
                .setMaxUpdates(1)
                .setWaitForAccurateLocation(true)
                .build()

            val callback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val ubicacion = result.lastLocation ?: return
                    lat = ubicacion.latitude
                    lng = ubicacion.longitude
                    actualizarDireccion(lat, lng)
                    locationLoaded = true
                    fusedLocation.removeLocationUpdates(this)
                }
            }

            fusedLocation.requestLocationUpdates(
                locationRequest,
                callback,
                android.os.Looper.getMainLooper()
            )
        }
    }

    // Carga el distrito del cliente y solicita permisos de ubicación al abrir
    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc -> clientDistrict = doc.getString("district") ?: "" }
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun publishRequest() {
        if (serviceType.isEmpty()) { errorMessage = "Selecciona el tipo de servicio"; return }
        if (!estaDentroDeLima(lat, lng)) { errorMessage = "Solo atendemos en Lima por ahora"; return }
        if (description.length < 20) { errorMessage = "La descripción debe tener al menos 20 caracteres"; return }

        isLoading = true
        errorMessage = ""

        fun saveRequest(imageUrl: String) {
            val requestId = UUID.randomUUID().toString()
            val request = RequestModel(
                requestId = requestId,
                clientId = uid,
                serviceType = serviceType,
                description = description,
                imageUrls = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList(),
                lat = lat,
                lng = lng,
                address = address,
                district = clientDistrict,
                reference = reference,
                status = "pendiente",
                isUrgent = isUrgent,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            db.collection("requests").document(requestId).set(request)
                .addOnSuccessListener {
                    // Notifica a técnicos con la especialidad solicitada
                    db.collection("users")
                        .whereEqualTo("role", "technician")
                        .whereArrayContains("specialties", serviceType)
                        .get()
                        .addOnSuccessListener { tecnicos ->
                            tecnicos.documents.forEach { tecnico ->
                                val tecId = tecnico.getString("uid") ?: return@forEach
                                notificationsRepo.crearNotificacion(
                                    userId = tecId,
                                    titulo = "Nueva solicitud de $serviceType",
                                    cuerpo = "Un cliente necesita ayuda en ${clientDistrict.ifEmpty { "Lima" }}",
                                    tipo = "nueva_solicitud",
                                    requestId = requestId
                                )
                            }
                        }
                    isLoading = false
                    navController.navigate(Routes.HOME_CLIENT) {
                        popUpTo(Routes.NEW_REQUEST) { inclusive = true }
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                    errorMessage = "Error al publicar la solicitud"
                }
        }

        if (photoUri != null) {
            scope.launch {
                val result = CloudinaryUploader.uploadImage(
                    context = context, uri = photoUri!!, folder = "homefix/requests"
                )
                result.fold(
                    onSuccess = { url -> saveRequest(url) },
                    onFailure = { saveRequest("") }
                )
            }
        } else {
            saveRequest("")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (pantallaAncha) 20.dp else 14.dp,
                    vertical = 20.dp
                )
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header con botón volver
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nueva solicitud",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (pantallaAncha) 24.sp else 20.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Tipo de servicio — dropdown con íconos emoji ──
            Text(
                text = "Tipo de servicio",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = dropdownExpandido,
                onExpandedChange = { dropdownExpandido = !dropdownExpandido }
            ) {
                OutlinedTextField(
                    value = if (serviceType.isEmpty()) "" else "${iconoPorEspecialidad(serviceType)}  $serviceType",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = {
                        Text(
                            "Selecciona un servicio...",
                            color = TextHint,
                            fontSize = if (pantallaAncha) 14.sp else 13.sp
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpandido)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = if (serviceType.isEmpty()) CardBorder else Primary.copy(alpha = 0.5f),
                        focusedContainerColor = Primary.copy(alpha = 0.03f),
                        unfocusedContainerColor = if (serviceType.isEmpty()) Color.Transparent else Primary.copy(alpha = 0.03f)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = if (pantallaAncha) 15.sp else 13.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpandido,
                    onDismissRequest = { dropdownExpandido = false }
                ) {
                    serviceTypes.forEach { tipo ->
                        val estaSeleccionado = serviceType == tipo
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = iconoPorEspecialidad(tipo),
                                        fontSize = 18.sp,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Text(
                                        text = tipo,
                                        color = if (estaSeleccionado) Primary else TextPrimary,
                                        fontWeight = if (estaSeleccionado) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = if (pantallaAncha) 14.sp else 13.sp
                                    )
                                }
                            },
                            onClick = {
                                serviceType = tipo
                                dropdownExpandido = false
                            },
                            modifier = Modifier.background(
                                if (estaSeleccionado) Primary.copy(alpha = 0.07f) else Color.Transparent
                            ),
                            trailingIcon = {
                                if (estaSeleccionado) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Descripción del problema ──
            Text(
                text = "Describe el problema",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (pantallaAncha) 120.dp else 100.dp),
                placeholder = {
                    Text(
                        "Ej: El tomacorriente de la cocina no funciona, hay un corto...",
                        color = TextHint,
                        fontSize = if (pantallaAncha) 14.sp else 12.sp
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = CardBorder
                )
            )
            Text(
                text = "${description.length}/500",
                style = MaterialTheme.typography.labelSmall,
                color = if (description.length > 450) Error else TextSecondary,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Foto del problema (opcional) ──
            Text(
                text = "Foto del problema (opcional)",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (photoUri != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Foto del problema",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (pantallaAncha) 200.dp else 160.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { photoUri = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Quitar foto", tint = Color.White)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cámara", fontSize = if (pantallaAncha) 14.sp else 12.sp)
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galería", fontSize = if (pantallaAncha) 14.sp else 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Ubicación del servicio ──
            Text(
                text = "Ubicación del servicio",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Buscador con sugerencias en tiempo real
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { texto ->
                        busqueda = texto
                        buscarSugerencias(texto)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Buscar dirección en Lima...",
                            color = TextHint,
                            fontSize = if (pantallaAncha) 14.sp else 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (busqueda.isNotEmpty()) {
                            IconButton(onClick = {
                                busqueda = ""
                                sugerencias = emptyList()
                                mostrarSugerencias = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = CardBorder
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (sugerencias.isNotEmpty()) seleccionarSugerencia(sugerencias[0])
                    })
                )
            }

            // Lista de sugerencias — aparece debajo del buscador con animación
            AnimatedVisibility(
                visible = mostrarSugerencias,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        sugerencias.forEachIndexed { index, resultado ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { seleccionarSugerencia(resultado) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = resultado.featureName ?: resultado.getAddressLine(0) ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = if (pantallaAncha) 14.sp else 12.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = resultado.getAddressLine(0) ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = if (pantallaAncha) 12.sp else 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (index < sugerencias.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = CardBorder
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mapa interactivo OSMDroid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                MapaUbicacion(
                    lat = lat,
                    lng = lng,
                    onUbicacionSeleccionada = { nuevoLat, nuevoLng ->
                        val cambioSignificativo =
                            Math.abs(lat - nuevoLat) > 0.0001 || Math.abs(lng - nuevoLng) > 0.0001
                        if (cambioSignificativo) {
                            lat = nuevoLat
                            lng = nuevoLng
                            actualizarDireccion(lat, lng)
                            locationLoaded = true
                            errorMessage = ""
                        }
                    },
                    onFueraDeCobertura = {
                        errorMessage = "Solo atendemos en Lima por ahora"
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tarjeta con la dirección detectada
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (locationLoaded) Success.copy(alpha = 0.08f) else SurfaceVariant
                )
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (locationLoaded) Success else TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locationLoaded) address else "Mueve el pin para definir la ubicación",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (locationLoaded) TextPrimary else TextSecondary,
                        fontSize = if (pantallaAncha) 13.sp else 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de referencia de dirección
            HomefixTextField(
                value = reference,
                onValueChange = { reference = it },
                label = "Referencia (opcional)",
                singleLine = false
            )
            Text(
                text = "Ej: Frente al parque, casa de rejas azules",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Toggle urgente ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUrgent) Error.copy(alpha = 0.08f) else SurfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚡ Urgente",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isUrgent) Error else TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = if (pantallaAncha) 16.sp else 14.sp
                        )
                        Text(
                            text = "Prioridad alta para los técnicos",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = if (pantallaAncha) 13.sp else 11.sp
                        )
                    }
                    Switch(
                        checked = isUrgent,
                        onCheckedChange = { isUrgent = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Error)
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HomefixButton(
                text = "Publicar solicitud",
                onClick = { publishRequest() },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}