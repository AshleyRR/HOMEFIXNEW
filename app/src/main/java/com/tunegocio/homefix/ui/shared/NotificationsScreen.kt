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

// NUEVO - MULTIDIOMA:
// Permite obtener textos y plurales desde strings_shared.xml.
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tunegocio.homefix.data.local.database.LocalDatabase
import com.tunegocio.homefix.data.model.NotificationModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*
import com.tunegocio.homefix.viewmodel.NotificationsViewModel

// NUEVO - MULTIDIOMA:
// Permite acceder a las claves del módulo shared.
import com.tunegocio.homefix.R
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
                    // MODIFICADO - MULTIDIOMA:
                    contentDescription = stringResource(R.string.shared_notifications_back),
                    tint = textColor
                )
            }
            Text(
                // MODIFICADO - MULTIDIOMA:
                text = stringResource(R.string.shared_notifications_title),
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
                        // MODIFICADO - MULTIDIOMA:
                        text = pluralStringResource(
                            R.plurals.shared_notifications_new_count,
                            noLeidas,
                            noLeidas
                        ),
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
                        // MODIFICADO - MULTIDIOMA:
                        text = stringResource(R.string.shared_notifications_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        // MODIFICADO - MULTIDIOMA:
                        text = stringResource(R.string.shared_notifications_empty_description),
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
                                    "cliente_cancelo" ->
                                        navController.navigate(Routes.HOME_TECHNICIAN)
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
        "cliente_cancelo" -> NotifStyle(Icons.Default.Cancel, Error)
        else -> NotifStyle(Icons.Default.Notifications, TextSecondary)
    }
}

// NUEVO - MULTIDIOMA:
// Traduce la etiqueta corta según el tipo interno de la notificación.
@Composable
private fun sharedNotificationLabel(type: String): String {
    return when (type) {
        "nueva_solicitud" -> stringResource(R.string.shared_notification_label_new_request)
        "tecnico_aceptado" -> stringResource(R.string.shared_notification_label_interested)
        "tecnico_elegido" -> stringResource(R.string.shared_notification_label_selected)
        "tecnico_rechazado" -> stringResource(R.string.shared_notification_label_not_selected)
        "tecnico_cancelo" -> stringResource(R.string.shared_notification_label_technician_canceled)
        "cliente_cancelo" -> stringResource(R.string.shared_notification_label_client_canceled)
        "en_camino" -> stringResource(R.string.shared_notification_label_on_the_way)
        "completado" -> stringResource(R.string.shared_notification_label_completed)
        "confirmar_completado" -> stringResource(R.string.shared_notification_label_confirm_work)
        "confirmar_sin_continuar" -> stringResource(R.string.shared_notification_label_confirm_closure)
        "completado_rechazado" -> stringResource(R.string.shared_notification_label_work_rejected)
        "sin_continuar_confirmado" -> stringResource(R.string.shared_notification_label_process_closed)
        else -> stringResource(R.string.shared_notification_label_default)
    }
}

// NUEVO - MULTIDIOMA:
// Traduce el título visible según type. No cambia el título guardado en Firebase.
@Composable
private fun sharedNotificationTitle(type: String, fallback: String): String {
    return when (type) {
        "nueva_solicitud" -> stringResource(R.string.shared_notification_title_new_request)
        "tecnico_aceptado" -> stringResource(R.string.shared_notification_title_interested)
        "tecnico_elegido" -> stringResource(R.string.shared_notification_title_selected)
        "tecnico_rechazado" -> stringResource(R.string.shared_notification_title_not_selected)
        "tecnico_cancelo" -> stringResource(R.string.shared_notification_title_technician_canceled)
        "cliente_cancelo" -> stringResource(R.string.shared_notification_title_client_canceled)
        "en_camino" -> stringResource(R.string.shared_notification_title_on_the_way)
        "completado" -> stringResource(R.string.shared_notification_title_completed)
        "confirmar_completado" -> stringResource(R.string.shared_notification_title_confirm_work)
        "confirmar_sin_continuar" -> stringResource(R.string.shared_notification_title_confirm_closure)
        "completado_rechazado" -> stringResource(R.string.shared_notification_title_work_rejected)
        "sin_continuar_confirmado" -> stringResource(R.string.shared_notification_title_process_closed)
        else -> fallback
    }
}

// NUEVO - MULTIDIOMA:
// Traduce el cuerpo visible según type. Se usa un texto genérico localizado
// porque las notificaciones antiguas guardaron el cuerpo completo en español.
@Composable
private fun sharedNotificationBody(type: String, fallback: String): String {
    return when (type) {
        "nueva_solicitud" -> stringResource(R.string.shared_notification_body_new_request)
        "tecnico_aceptado" -> stringResource(R.string.shared_notification_body_interested)
        "tecnico_elegido" -> stringResource(R.string.shared_notification_body_selected)
        "tecnico_rechazado" -> stringResource(R.string.shared_notification_body_not_selected)
        "tecnico_cancelo" -> stringResource(R.string.shared_notification_body_technician_canceled)
        "cliente_cancelo" -> stringResource(R.string.shared_notification_body_client_canceled)
        "en_camino" -> stringResource(R.string.shared_notification_body_on_the_way)
        "completado" -> stringResource(R.string.shared_notification_body_completed)
        "confirmar_completado" -> stringResource(R.string.shared_notification_body_confirm_work)
        "confirmar_sin_continuar" -> stringResource(R.string.shared_notification_body_confirm_closure)
        "completado_rechazado" -> stringResource(R.string.shared_notification_body_work_rejected)
        "sin_continuar_confirmado" -> stringResource(R.string.shared_notification_body_process_closed)
        else -> fallback
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

    // NUEVO - MULTIDIOMA:
    // Los títulos y cuerpos antiguos pueden estar almacenados en Firebase en español.
    // Se muestran traducidos según el campo interno type, sin modificar Firestore.
    // Para tipos desconocidos se conserva el texto original como respaldo.
    val tituloVisible = sharedNotificationTitle(
        type = notificacion.type,
        fallback = notificacion.title
    )
    val cuerpoVisible = sharedNotificationBody(
        type = notificacion.type,
        fallback = notificacion.body
    )
    val etiqueta = sharedNotificationLabel(notificacion.type)

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
                        text = tituloVisible,
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
                    text = cuerpoVisible,
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