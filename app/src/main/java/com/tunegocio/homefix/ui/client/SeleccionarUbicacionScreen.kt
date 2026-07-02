package com.tunegocio.homefix.ui.client

import android.location.Geocoder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.tunegocio.homefix.ui.components.MapaUbicacion
import com.tunegocio.homefix.ui.components.estaDentroDeLima
import com.tunegocio.homefix.ui.theme.*
import com.tunegocio.homefix.viewmodel.UbicacionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder
import java.util.*
import android.annotation.SuppressLint

import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.google.android.gms.location.LocationServices


@SuppressLint("MissingPermission")
@Composable
fun SeleccionarUbicacionScreen(
    navController: NavController,
    ubicacionViewModel: UbicacionViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val latInicial by ubicacionViewModel.lat.collectAsState()
    val lngInicial by ubicacionViewModel.lng.collectAsState()

    var lat by remember { mutableStateOf(latInicial) }
    var lng by remember { mutableStateOf(lngInicial) }
    var address by remember { mutableStateOf("") }
    var locationLoaded by remember { mutableStateOf(false) }

    var busqueda by remember { mutableStateOf("") }
    var sugerencias by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var mostrarSugerencias by remember { mutableStateOf(false) }
    var jobBusqueda by remember { mutableStateOf<Job?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var mostrarDialogoGps by remember { mutableStateOf(false) }

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    fun actualizarDireccion(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(context, Locale("es", "PE"))
            @Suppress("DEPRECATION")
            val resultados = geocoder.getFromLocation(latitude, longitude, 1)
            if (!resultados.isNullOrEmpty()) {
                address = resultados[0].getAddressLine(0)
                    ?: "Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}"
                locationLoaded = true
            }
        } catch (e: Exception) {
            address = "Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}"
            locationLoaded = true
        }
    }

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
                sugerencias = emptyList()
                mostrarSugerencias = false
            }
        }
    }

    fun seleccionarSugerencia(resultado: android.location.Address) {
        lat = resultado.latitude
        lng = resultado.longitude
        address = resultado.getAddressLine(0) ?: ""
        locationLoaded = true
        mostrarSugerencias = false
        sugerencias = emptyList()
        busqueda = resultado.featureName ?: address
    }

    // Diálogo para activar GPS
    if (mostrarDialogoGps) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoGps = false },
            icon = {
                Icon(
                    Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Ubicación desactivada",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Para usar tu ubicación actual necesitas activar el GPS de tu dispositivo.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoGps = false
                        // Abre configuración de ubicación del celular
                        context.startActivity(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Activar GPS", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoGps = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = bgColor,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = textColor)
                        }
                        Text(
                            text = "Selecciona tu ubicación",
                            style = MaterialTheme.typography.titleLarge,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = {
                            busqueda = it
                            buscarSugerencias(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar dirección en Lima...", color = TextHint) },
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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    AnimatedVisibility(visible = mostrarSugerencias, enter = fadeIn(), exit = fadeOut()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(resultado.featureName ?: "", style = MaterialTheme.typography.bodyMedium, color = textColor, fontWeight = FontWeight.Medium, maxLines = 1)
                                            Text(resultado.getAddressLine(0) ?: "", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                                        }
                                    }
                                    if (index < sugerencias.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CardBorder)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = bgColor,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    if (errorMessage.isNotEmpty()) {
                        Text(text = errorMessage, color = Error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (locationLoaded) Success.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (locationLoaded) Success else TextSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (locationLoaded) address else "Mueve el pin para definir la ubicación",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (locationLoaded) textColor else TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            ubicacionViewModel.actualizarUbicacion(lat, lng, address)
                            ubicacionViewModel.confirmar()
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        enabled = locationLoaded
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar ubicación", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { paddingValues ->
        // Mapa respeta el espacio del header y footer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MapaUbicacion(
                lat = lat,
                lng = lng,
                onUbicacionSeleccionada = { nuevoLat, nuevoLng ->
                    val cambio = Math.abs(lat - nuevoLat) > 0.0001 || Math.abs(lng - nuevoLng) > 0.0001
                    if (cambio) {
                        lat = nuevoLat
                        lng = nuevoLng
                        actualizarDireccion(lat, lng)
                    }
                },
                onFueraDeCobertura = { errorMessage = "Solo atendemos en Lima por ahora" },
                modifier = Modifier.fillMaxSize()
            )

            FloatingActionButton(
                onClick = {
                    // Verificar si el GPS está activo antes de obtener ubicación
                    val locationManager = context.getSystemService(
                        android.content.Context.LOCATION_SERVICE
                    ) as LocationManager

                    val gpsActivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val redActiva = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if (!gpsActivo && !redActiva) {
                        // GPS desactivado — mostrar diálogo
                        mostrarDialogoGps = true
                    } else {
                        // GPS activo — obtener ubicación
                        try {
                            val fusedLocation = LocationServices
                                .getFusedLocationProviderClient(context)

                            val locationRequest = com.google.android.gms.location.CurrentLocationRequest.Builder()
                                .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                                .setDurationMillis(5000)
                                .build()

                            fusedLocation.getCurrentLocation(locationRequest, null)
                                .addOnSuccessListener { location ->
                                    if (location != null) {
                                        lat = location.latitude
                                        lng = location.longitude
                                        actualizarDireccion(lat, lng)
                                    } else {
                                        errorMessage = "No se pudo obtener la ubicación, intenta de nuevo"
                                    }
                                }
                                .addOnFailureListener {
                                    errorMessage = "Error al obtener ubicación"
                                }
                        } catch (e: SecurityException) {
                            errorMessage = "Activa el permiso de ubicación"
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Mi ubicación"
                )

            }

        }
    }
}