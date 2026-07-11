package com.tunegocio.homefix.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

// Coordenadas del centro de Lima
private val LIMA_CENTRO = LatLng(-12.0464, -77.0428)
private const val ZOOM_INICIAL = 15f

// Valida si una coordenada cae dentro del área de cobertura de Lima
fun estaDentroDeLima(lat: Double, lng: Double): Boolean {
    return lat in -12.5..-11.7 && lng in -77.2..-76.7
}

// Composable que muestra el mapa con pin central y notifica la ubicación seleccionada
@Composable
fun MapaUbicacion(
    lat: Double,
    lng: Double,
    onUbicacionSeleccionada: (Double, Double) -> Unit,
    onFueraDeCobertura: () -> Unit,
    modifier: Modifier = Modifier
) {
    val posicionInicial = if (lat != 0.0 && lng != 0.0)
        LatLng(lat, lng) else LIMA_CENTRO

    val camaraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(posicionInicial, ZOOM_INICIAL)
    }

    // Mueve la cámara cuando lat/lng cambian desde una búsqueda externa
    LaunchedEffect(lat, lng) {
        if (lat != 0.0 && lng != 0.0) {
            camaraState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), ZOOM_INICIAL)
            )
        }
    }

    // Notifica la ubicación seleccionada cuando la cámara deja de moverse
    LaunchedEffect(camaraState.isMoving) {
        if (!camaraState.isMoving) {
            val centro = camaraState.position.target
            if (estaDentroDeLima(centro.latitude, centro.longitude)) {
                onUbicacionSeleccionada(centro.latitude, centro.longitude)
            } else {
                onFueraDeCobertura()
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Mapa de Google Maps
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camaraState,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = true
            )
        )

        // Pin fijo en el centro
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Ubicación seleccionada",
            tint = Color.Red,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )
    }
}