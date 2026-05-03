package com.tunegocio.homefix.ui.shared

import androidx.compose.foundation.background
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tunegocio.homefix.data.model.NotificationModel
import com.tunegocio.homefix.ui.theme.*
import com.tunegocio.homefix.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.tunegocio.homefix.navigation.Routes
import androidx.compose.foundation.clickable

@Composable
fun NotificationsScreen(navController: NavController) {

    val viewModel: NotificationsViewModel = viewModel()
    val notificaciones by viewModel.notificaciones.collectAsState()
    val noLeidas by viewModel.noLeidas.collectAsState()

    // ── Colores dinámicos ─────────────────────────────────────
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        viewModel.marcarTodasComoLeidas()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = textColor)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = "Notificaciones",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (noLeidas > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primaryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$noLeidas nuevas",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (notificaciones.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔔", style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sin notificaciones",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Aquí aparecerán tus alertas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notificaciones) { notificacion ->
                    NotificacionCard(
                        notificacion = notificacion,
                        onClick = {
                            if (notificacion.requestId.isNotEmpty()) {
                                when (notificacion.type) {
                                    "tecnico_aceptado",
                                    "en_camino",
                                    "completado",
                                    "tecnico_cancelo",
                                    "confirmar_completado",
                                    "confirmar_sin_continuar" ->
                                        navController.navigate(
                                            Routes.requestTracking(notificacion.requestId)
                                        )
                                    else ->
                                        navController.navigate(
                                            Routes.requestDetail(notificacion.requestId)
                                        )
                                }
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun NotificacionCard(
    notificacion: NotificationModel,
    onClick: () -> Unit = {}
) {
    // ── Colores dinámicos ─────────────────────────────────────
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val hintText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary

    val fechaFormato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val fecha = fechaFormato.format(Date(notificacion.createdAt))

    val (emoji, color) = when (notificacion.type) {
        "nueva_solicitud"          -> "🔧" to primaryColor
        "tecnico_aceptado"         -> "👋" to Info
        "tecnico_elegido"          -> "🎉" to Success
        "tecnico_rechazado"        -> "❌" to MaterialTheme.colorScheme.error
        "tecnico_cancelo"          -> "🚫" to MaterialTheme.colorScheme.error
        "en_camino"                -> "🚗" to Warning
        "completado"               -> "✅" to Success
        "confirmar_completado"     -> "✅" to Warning
        "confirmar_sin_continuar"  -> "⚠️" to MaterialTheme.colorScheme.error
        "completado_rechazado"     -> "❌" to MaterialTheme.colorScheme.error
        "sin_continuar_confirmado" -> "🚫" to secondaryText
        else                       -> "🔔" to secondaryText
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notificacion.isRead)
                primaryColor.copy(alpha = 0.08f)
            else
                surfaceColor
        ),
        border = if (!notificacion.isRead)
            androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, outlineColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notificacion.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notificacion.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = primaryColor
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = notificacion.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = when (notificacion.type) {
                                "nueva_solicitud"          -> "Nueva solicitud"
                                "tecnico_aceptado"         -> "Técnico interesado"
                                "tecnico_elegido"          -> "¡Te eligieron!"
                                "tecnico_rechazado"        -> "No seleccionado"
                                "tecnico_cancelo"          -> "Técnico canceló"
                                "en_camino"                -> "En camino"
                                "completado"               -> "Completado"
                                "confirmar_completado"     -> "Confirmar trabajo"
                                "confirmar_sin_continuar"  -> "Confirmar cierre"
                                "completado_rechazado"     -> "Trabajo rechazado"
                                "sin_continuar_confirmado" -> "Proceso cerrado"
                                else                       -> "Notificación"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = fecha,
                        style = MaterialTheme.typography.labelSmall,
                        color = hintText
                    )
                }
            }
        }
    }
}