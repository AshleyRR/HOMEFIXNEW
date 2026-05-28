package com.tunegocio.homefix.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

// Coordenadas del centro de Lima
private val LIMA_CENTRO = GeoPoint(-12.0464, -77.0428)
private const val ZOOM_INICIAL = 15.0

// Verifica que el punto esté dentro de Lima
fun estaDentroDeLima(lat: Double, lng: Double): Boolean {
    return lat in -12.5..-11.7 && lng in -77.2..-76.7
}

@Composable
fun MapaUbicacion(
    lat: Double,
    lng: Double,
    // Se llama cuando el centro del mapa cambia al moverlo
    onUbicacionSeleccionada: (Double, Double) -> Unit,
    onFueraDeCobertura: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Referencia al MapView para moverlo desde búsqueda
    var mapaRef by remember { mutableStateOf<MapView?>(null) }

    // Cuando cambian lat/lng desde búsqueda de texto, mueve el mapa
    LaunchedEffect(lat, lng) {
        mapaRef?.let { mapa ->
            val punto = GeoPoint(lat, lng)
            mapa.controller.animateTo(punto)
        }
    }

    // Cuadro para superponer el pin fijo encima del mapa
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Mapa OSMDroid sin conflicto de scroll
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                Configuration.getInstance().userAgentValue = context.packageName

                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setBuiltInZoomControls(false)
                    setMultiTouchControls(true)

                    controller.setZoom(ZOOM_INICIAL)
                    controller.setCenter(
                        if (lat != 0.0 && lng != 0.0)
                            GeoPoint(lat, lng)
                        else
                            LIMA_CENTRO
                    )

                    // Al terminar el primer layout, notifica la ubicación inicial
                    addOnFirstLayoutListener { _, _, _, _, _ ->
                        val centro = mapCenter
                        if (estaDentroDeLima(centro.latitude, centro.longitude)) {
                            onUbicacionSeleccionada(centro.latitude, centro.longitude)
                        } else {
                            onFueraDeCobertura()
                        }
                    }

                    // Único setOnTouchListener — resuelve el lag del Column
                    // y notifica la ubicación solo cuando el dedo se levanta
                    setOnTouchListener { v, evento ->
                        when (evento.action) {
                            android.view.MotionEvent.ACTION_DOWN,
                            android.view.MotionEvent.ACTION_MOVE -> {
                                // Bloquea el scroll del Column padre mientras el dedo está en el mapa
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            android.view.MotionEvent.ACTION_UP,
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                // Devuelve el control al Column cuando el dedo se levanta
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                                // Lee el centro solo cuando el gesto termina — evita recomposiciones infinitas
                                val centro = mapCenter
                                if (estaDentroDeLima(centro.latitude, centro.longitude)) {
                                    onUbicacionSeleccionada(centro.latitude, centro.longitude)
                                } else {
                                    onFueraDeCobertura()
                                }
                            }
                        }
                        false // false para que el MapView siga procesando el gesto normalmente
                    }

                    mapaRef = this
                }
            }

        )

        // Pin fijo en el centro  — aguja con punto rojo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-20).dp)
        ) {
            // Círculo rojo con punto blanco al centro
            Canvas(modifier = Modifier.size(14.dp)) {
                drawCircle(
                    color = Color.Red,
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = Color.White,
                    radius = size.minDimension / 4
                )
            }
            // Línea vertical de la aguja
            Canvas(
                modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
            ) {
                drawRect(color = Color.Red)
            }
            // Sombra pequeña en el suelo
            Text(
                text = "•",
                color = Color.Black.copy(alpha = 0.3f),
                fontSize = 8.sp
            )
        }
    }
}