package com.tunegocio.homefix.ui.shared

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.tunegocio.homefix.R
import com.tunegocio.homefix.data.UserPreferences
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*
import kotlinx.coroutines.launch

// Pantalla de ajustes: apariencia, idioma, notificaciones, cuenta y cierre de sesión
@Composable
fun SettingsScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    val darkMode by prefs.darkMode.collectAsState(initial = false)
    // Código de idioma guardado ("es", "en" o "pt"), por defecto "es"
    val language by prefs.language.collectAsState(initial = "es")
    val notifSound by prefs.notifSound.collectAsState(initial = true)
    val notifVibration by prefs.notifVibration.collectAsState(initial = true)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    // Colores dinámicos
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    // Traduce el código de idioma guardado al nombre visible
    val languageLabel = when (language) {
        "en" -> stringResource(R.string.settings_language_english)
        "pt" -> stringResource(R.string.settings_language_portuguese)
        else -> stringResource(R.string.settings_language_spanish_peru)
    }

    // Diálogo selector de idioma: guarda el código elegido en UserPreferences
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text(
                    text = stringResource(R.string.settings_select_language),
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            },
            text = {
                Column {
                    // Código fijo para UserPreferences + nombre traducido
                    listOf(
                        "es" to stringResource(R.string.settings_language_option_spanish),
                        "en" to stringResource(R.string.settings_language_option_english),
                        "pt" to stringResource(R.string.settings_language_option_portuguese)
                    ).forEach { (code, label) ->
                        val isSelected = language == code
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Primary else textColor,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    scope.launch { prefs.setLanguage(code) }
                                    showLanguageDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(
                        text = stringResource(R.string.settings_close),
                        color = secondaryText
                    )
                }
            }
        )
    }
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.terms_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.terms_full_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTermsDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.terms_close)
                    )
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sección apariencia
            SettingsSectionTitle(
                title = stringResource(R.string.settings_appearance),
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                iconColor = if (darkMode) Color(0xFF90CAF9) else Warning,
                title = if (darkMode) {
                    stringResource(R.string.settings_dark_mode)
                } else {
                    stringResource(R.string.settings_light_mode)
                },
                subtitle = if (darkMode) {
                    stringResource(R.string.settings_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                checked = darkMode,
                onCheckedChange = { scope.launch { prefs.setDarkMode(it) } },
                titleColor = textColor,
                subtitleColor = secondaryText,
                cardColor = surfaceColor,
                borderColor = outlineColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Abre el selector de idioma
            SettingsClickItem(
                icon = Icons.Default.Language,
                iconColor = Info,
                title = stringResource(R.string.settings_language),
                subtitle = languageLabel,
                onClick = { showLanguageDialog = true },
                titleColor = textColor,
                subtitleColor = secondaryText,
                cardColor = surfaceColor,
                borderColor = outlineColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección notificaciones
            SettingsSectionTitle(
                title = stringResource(R.string.settings_notifications),
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = Icons.Default.VolumeUp,
                iconColor = Secondary,
                title = stringResource(R.string.settings_sound),
                subtitle = if (notifSound) {
                    stringResource(R.string.settings_sound_enabled)
                } else {
                    stringResource(R.string.settings_no_sound)
                },
                checked = notifSound,
                onCheckedChange = { scope.launch { prefs.setNotifSound(it) } },
                titleColor = textColor,
                subtitleColor = secondaryText,
                cardColor = surfaceColor,
                borderColor = outlineColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = Icons.Default.Vibration,
                iconColor = TechnicianColor,
                title = stringResource(R.string.settings_vibration),
                subtitle = if (notifVibration) {
                    stringResource(R.string.settings_vibration_enabled)
                } else {
                    stringResource(R.string.settings_no_vibration)
                },
                checked = notifVibration,
                onCheckedChange = { scope.launch { prefs.setNotifVibration(it) } },
                titleColor = textColor,
                subtitleColor = secondaryText,
                cardColor = surfaceColor,
                borderColor = outlineColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección cuenta
            SettingsSectionTitle(
                title = stringResource(R.string.settings_account),
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsClickItem(
                icon = Icons.Default.Lock,
                iconColor = Primary,
                title = stringResource(R.string.settings_privacy),
                subtitle = stringResource(R.string.settings_terms_and_conditions),
                onClick = {
                    showTermsDialog = true
                },
                titleColor = textColor,
                subtitleColor = secondaryText,
                cardColor = surfaceColor,
                borderColor = outlineColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Cierra sesión y regresa al login
            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_logout),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Título de sección en mayúsculas (Apariencia, Notificaciones, Cuenta)
@Composable
private fun SettingsSectionTitle(title: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

// Item de ajuste con switch (ej. modo oscuro, sonido, vibración)
@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    titleColor: Color,
    subtitleColor: Color,
    cardColor: Color,
    borderColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = iconColor.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = titleColor, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = subtitleColor)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(alpha = 0.4f))
            )
        }
    }
}

// Item de ajuste clickeable que navega o abre un diálogo (ej. idioma, privacidad)
@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: Color,
    subtitleColor: Color,
    cardColor: Color,
    borderColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = iconColor.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = titleColor, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = subtitleColor)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(20.dp))
        }
    }
}