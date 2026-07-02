package com.tunegocio.homefix.ui.client

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.tunegocio.homefix.ui.components.estaDentroDeLima
import com.tunegocio.homefix.ui.theme.*
import kotlinx.coroutines.launch

import java.io.File
import java.util.*

import com.tunegocio.homefix.viewmodel.UbicacionViewModel
import com.tunegocio.homefix.viewmodel.SolicitudViewModel

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

@SuppressLint("MissingPermission")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NewRequestScreen(
    navController: NavController,
    ubicacionViewModel: UbicacionViewModel,
    solicitudViewModel: SolicitudViewModel
) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

    val notificationsRepo = remember { com.tunegocio.homefix.data.NotificationsRepository() }

    val configuracion = LocalConfiguration.current
    val pantallaAncha = configuracion.screenWidthDp >= 360

    /*var serviceType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }*/

    val serviceType by solicitudViewModel.serviceType.collectAsState()
    val description by solicitudViewModel.description.collectAsState()
    val reference by solicitudViewModel.reference.collectAsState()
    val isUrgent by solicitudViewModel.isUrgent.collectAsState()
    val photoUrisStrings by solicitudViewModel.photoUris.collectAsState()
    val photoUris = photoUrisStrings.map { Uri.parse(it) }

    var address by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf(-12.0464) }
    var lng by remember { mutableStateOf(-77.0428) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var locationLoaded by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var clientDistrict by remember { mutableStateOf("") }
    var dropdownExpandido by remember { mutableStateOf(false) }



    val serviceTypes = ALL_SPECIALTIES

    val photoFile = remember { File(context.cacheDir, "photo_${UUID.randomUUID()}.jpg") }
    val photoUriForCamera = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUris.size < 2) {
            solicitudViewModel.addPhoto(photoUriForCamera)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (photoUris.size < 2) {
                solicitudViewModel.addPhoto(it)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) cameraLauncher.launch(photoUriForCamera) }

    val ubicacionConfirmada by ubicacionViewModel.confirmada.collectAsState()
    val latViewModel by ubicacionViewModel.lat.collectAsState()
    val lngViewModel by ubicacionViewModel.lng.collectAsState()
    val addressViewModel by ubicacionViewModel.address.collectAsState()

    // Cuando vuelve de la pantalla de ubicación actualiza los datos
    LaunchedEffect(ubicacionConfirmada) {
        if (ubicacionConfirmada) {
            lat = latViewModel
            lng = lngViewModel
            address = addressViewModel
            locationLoaded = true
            ubicacionViewModel.resetConfirmacion()
        }
    }

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


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            val fusedLocation = LocationServices.getFusedLocationProviderClient(context)
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

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc -> clientDistrict = doc.getString("district") ?: "" }

        // Solo pedir GPS si no hay ubicación ya confirmada del mapa
        if (!locationLoaded) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
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
                    // limpiar el formulario
                    solicitudViewModel.limpiar()
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

        if (photoUris.isNotEmpty()) {
            scope.launch {
                val urls = mutableListOf<String>()
                photoUris.forEach { uri ->
                    val result = CloudinaryUploader.uploadImage(
                        context = context,
                        uri = uri,
                        folder = "homefix/requests"
                    )
                    result.fold(
                        onSuccess = { url -> urls.add(url) },
                        onFailure = { }
                    )
                }
                saveRequest(urls.joinToString(","))
            }
        } else {
            saveRequest("")
        }
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nueva solicitud",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (pantallaAncha) 24.sp else 20.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tipo de servicio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = dropdownExpandido,
                onExpandedChange = { dropdownExpandido = !dropdownExpandido }
            ) {
                OutlinedTextField(
                    value = serviceType,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = {
                        Text(
                            "Selecciona un servicio...",
                            color = TextHint,
                            fontSize = if (pantallaAncha) 14.sp else 13.sp
                        )
                    },
                    leadingIcon = {
                        if (serviceType.isNotEmpty()) {
                            Icon(
                                imageVector = getServiceTypeIcon(serviceType) ?: Icons.Default.Build,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
                                    Icon(
                                        imageVector = getServiceTypeIcon(tipo),
                                        contentDescription = null,
                                        tint = if (estaSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = tipo,
                                        color = if (estaSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (estaSeleccionado) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = if (pantallaAncha) 14.sp else 13.sp
                                    )
                                }
                            },
                            onClick = {
                                solicitudViewModel.setServiceType(tipo)
                                dropdownExpandido = false
                            },
                            modifier = Modifier.background(
                                if (estaSeleccionado) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            ),
                            trailingIcon = {
                                if (estaSeleccionado) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Describe el problema",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) solicitudViewModel.setDescription(it) },
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

            Text(
                text = "Foto del problema (opcional)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Fotos seleccionadas
            if (photoUris.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    photoUris.forEachIndexed { index, uri ->
                        Box(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Foto ${index + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (pantallaAncha) 160.dp else 130.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    solicitudViewModel.removePhoto(index)
                                },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar foto",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    // Espacio vacío si solo hay 1 foto
                    if (photoUris.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Botones agregar foto — solo si hay menos de 2
            if (photoUris.size < 2) {
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
                // Indicador de cuántas fotos puede agregar
                Text(
                    text = "${photoUris.size}/2 fotos agregadas",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ubicación del servicio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))


            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    ubicacionViewModel.actualizarUbicacion(lat, lng, address)
                    navController.navigate(Routes.SELECCIONAR_UBICACION)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary.copy(alpha = 0.1f),
                    contentColor = Primary
                )
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Seleccionar ubicación en el mapa", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (locationLoaded) Success.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (locationLoaded) Success else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (locationLoaded) address else "Mueve el pin para definir la ubicación",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (locationLoaded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HomefixTextField(
                value = reference,
                onValueChange = { solicitudViewModel.setReference(it) },
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isUrgent) Error else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Urgente",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isUrgent) Error else TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = if (pantallaAncha) 16.sp else 14.sp
                            )
                        }
                        Text(
                            text = "Prioridad alta para los técnicos",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = if (pantallaAncha) 13.sp else 11.sp,
                            modifier = Modifier.padding(start = 28.dp, top = 4.dp)
                        )
                    }
                    Switch(
                        checked = isUrgent,
                        onCheckedChange = { solicitudViewModel.setIsUrgent(it) },
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