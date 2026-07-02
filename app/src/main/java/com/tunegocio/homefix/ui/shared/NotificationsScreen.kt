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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tunegocio.homefix.data.local.database.LocalDatabase
import com.tunegocio.homefix.data.model.NotificationModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*
import com.tunegocio.homefix.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(navController: NavController) {

    val context = LocalContext.current
    val localDb = LocalDatabase(context)
    val viewModel: NotificationsViewModel = viewModel()
    val notificaciones by viewModel.notificaciones.collectAsState()
    val noLeidas by viewModel.noLeidas.collectAsState()

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val primaryColor = MaterialTheme.colorScheme.primary

    // Marcar como leídas y guardar en SQLite
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        viewModel.marcarTodasComoLeidas()
    }

    // Guardar notificaciones en SQLite cuando llegan
    LaunchedEffect(notificaciones) {
        notificaciones.forEach { notif ->
            localDb.guardarNotificacion(
                notificacionId = notif.id,
                titulo = notif.title,
                mensaje = notif.body,
                tipo = notif.type,
                requestId = notif.requestId
            )

        }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = textColor
                )
            }
            Text(
                text = "Notificaciones",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (noLeidas > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primaryColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$noLeidas nuevas",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        if (notificaciones.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(36.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = secondaryText,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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

// Datos del ícono y color según tipo de notificación
private data class NotifStyle(
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
private fun notifStyle(type: String): NotifStyle {
    return when (type) {
        "nueva_solicitud" -> NotifStyle(Icons.Default.Build, Primary)
        "tecnico_aceptado" -> NotifStyle(Icons.Default.Handshake, Info)
        "tecnico_elegido" -> NotifStyle(Icons.Default.CheckCircle, Success)
        "tecnico_rechazado" -> NotifStyle(Icons.Default.Cancel, Error)
        "tecnico_cancelo" -> NotifStyle(Icons.Default.DoNotDisturb, Error)
        "en_camino" -> NotifStyle(Icons.Default.DirectionsCar, Warning)
        "completado" -> NotifStyle(Icons.Default.TaskAlt, Success)
        "confirmar_completado" -> NotifStyle(Icons.Default.HowToReg, Warning)
        "confirmar_sin_continuar" -> NotifStyle(Icons.Default.Warning, Error)
        "completado_rechazado" -> NotifStyle(Icons.Default.ThumbDown, Error)
        "sin_continuar_confirmado" -> NotifStyle(Icons.Default.Block, TextSecondary)
        else -> NotifStyle(Icons.Default.Notifications, TextSecondary)
    }
}

@Composable
fun NotificacionCard(
    notificacion: NotificationModel,
    onClick: () -> Unit = {}
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val hintText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary

    val fechaFormato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val fecha = fechaFormato.format(Date(notificacion.createdAt))

    val style = notifStyle(notificacion.type)

    val etiqueta = when (notificacion.type) {
        "nueva_solicitud" -> "Nueva solicitud"
        "tecnico_aceptado" -> "Técnico interesado"
        "tecnico_elegido" -> "Te eligieron"
        "tecnico_rechazado" -> "No seleccionado"
        "tecnico_cancelo" -> "Técnico canceló"
        "en_camino" -> "En camino"
        "completado" -> "Completado"
        "confirmar_completado" -> "Confirmar trabajo"
        "confirmar_sin_continuar" -> "Confirmar cierre"
        "completado_rechazado" -> "Trabajo rechazado"
        "sin_continuar_confirmado" -> "Proceso cerrado"
        else -> "Notificación"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notificacion.isRead)
                primaryColor.copy(alpha = 0.06f)
            else
                surfaceColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (!notificacion.isRead) 1.dp else 0.5.dp,
            color = if (!notificacion.isRead)
                primaryColor.copy(alpha = 0.3f)
            else
                outlineColor.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Ícono del tipo de notificación
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = style.color.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Título + punto de no leída
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

                // Cuerpo del mensaje
                Text(
                    text = notificacion.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Etiqueta de tipo + fecha
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = style.color.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = etiqueta,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = style.color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = hintText,
                            modifier = Modifier.size(12.dp)
                        )
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
}