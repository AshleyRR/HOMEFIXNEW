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
// NUEVO - MULTIDIOMA:
// Permite obtener el texto correspondiente al idioma activo desde los archivos XML
// values/strings_settings.xml, values-en/strings_settings.xml y
// values-pt-rBR/strings_settings.xml.
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
// NUEVO - MULTIDIOMA:
// R permite acceder a las claves de texto declaradas en los archivos de recursos XML.
import com.tunegocio.homefix.R
import com.tunegocio.homefix.data.UserPreferences
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    val darkMode by prefs.darkMode.collectAsState(initial = false)
    // IDIOMA:
    // Observa continuamente el idioma guardado en UserPreferences.
    // "es" = español, "en" = inglés y "pt" = portugués.
    // Se mantiene "es" como idioma predeterminado.
    val language by prefs.language.collectAsState(initial = "es")
    val notifSound by prefs.notifSound.collectAsState(initial = true)
    val notifVibration by prefs.notifVibration.collectAsState(initial = true)

    // IDIOMA:
    // Controla si se muestra o se oculta la ventana para elegir el idioma.
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    // Colores dinámicos
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    // IDIOMA:
    // Convierte el código guardado en UserPreferences en el nombre visible del idioma.
    // stringResource selecciona automáticamente el texto del XML correspondiente
    // al idioma activo de la aplicación.
    val languageLabel = when (language) {
        "en" -> stringResource(R.string.settings_language_english)
        "pt" -> stringResource(R.string.settings_language_portuguese)
        else -> stringResource(R.string.settings_language_spanish_peru)
    }

    // IDIOMA:
    // Diálogo que permite elegir entre español, inglés y portugués.
    // No cambia la lógica de navegación ni los botones; solo guarda el código del idioma.
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text(
                    // IDIOMA: título obtenido desde strings_settings.xml.
                    text = stringResource(R.string.settings_select_language),
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            },
            text = {
                Column {
                    // IDIOMA:
                    // Cada opción conserva un código fijo para UserPreferences y obtiene
                    // su nombre visible desde el archivo XML del idioma activo.
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
                                    // IDIOMA:
                                    // Guarda el código seleccionado sin alterar ninguna
                                    // otra preferencia ni proceso de la aplicación.
                                    scope.launch { prefs.setLanguage(code) }

                                    // Cierra únicamente el diálogo después de seleccionar.
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
                        // IDIOMA: texto del botón obtenido desde el XML correspondiente.
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
                    // MODIFICADO - MULTIDIOMA:
                    // Usa el mismo título traducido de strings_terms.xml
                    // que utiliza el diálogo del registro.
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
                        // MODIFICADO - MULTIDIOMA:
                        // El contenido legal ya no está escrito directamente en español.
                        // Se reutiliza terms_full_text de strings_terms.xml para mostrar
                        // español, inglés o portugués según el idioma activo.
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
                        // MODIFICADO - MULTIDIOMA:
                        // Reutiliza el botón traducido de strings_terms.xml.
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
                        // IDIOMA: descripción accesible traducida del botón volver.
                        contentDescription = stringResource(R.string.settings_back),
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    // IDIOMA: título principal obtenido desde los recursos traducidos.
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // APARIENCIA - MULTIDIOMA:
            // Solo se reemplazó el texto fijo por stringResource; la lógica no cambió.
            SettingsSectionTitle(
                title = stringResource(R.string.settings_appearance),
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                iconColor = if (darkMode) Color(0xFF90CAF9) else Warning,
                // IDIOMA:
                // Los textos cambian según el modo y el idioma, pero el Switch conserva
                // exactamente la misma lógica y función.
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

            // IDIOMA:
            // La tarjeta conserva su icono, diseño y acción. Solo usa textos traducibles.
            SettingsClickItem(
                icon = Icons.Default.Language,
                iconColor = Info,
                title = stringResource(R.string.settings_language),
                subtitle = languageLabel,
                // IDIOMA: abre el mismo diálogo selector; no modifica la navegación.
                onClick = { showLanguageDialog = true },
                titleColor = textColor,
                subtitleColor = secondaryText,
                cardColor = surfaceColor,
                borderColor = outlineColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // NOTIFICACIONES - MULTIDIOMA:
            // Se traduce únicamente el texto visible; sonido y vibración mantienen su lógica.
            SettingsSectionTitle(
                title = stringResource(R.string.settings_notifications),
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = Icons.Default.VolumeUp,
                iconColor = Secondary,
                // IDIOMA: textos del control de sonido obtenidos desde los XML.
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
                // IDIOMA: textos del control de vibración obtenidos desde los XML.
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

            // CUENTA - MULTIDIOMA:
            // Se traducen solo los textos visibles; las acciones permanecen intactas.
            SettingsSectionTitle(
                title = stringResource(R.string.settings_account),
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsClickItem(
                icon = Icons.Default.Lock,
                iconColor = Primary,
                // IDIOMA: textos de privacidad obtenidos desde los XML.
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
                    // IDIOMA:
                    // Solo cambia el texto visible. FirebaseAuth.signOut() y la navegación
                    // hacia LOGIN permanecen exactamente iguales.
                    text = stringResource(R.string.settings_logout),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

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