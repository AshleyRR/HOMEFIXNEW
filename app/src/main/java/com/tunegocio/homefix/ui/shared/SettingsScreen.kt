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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
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

    val languageLabel = when (language) {
        "en" -> "English"
        "pt" -> "Português"
        else -> "Español (Perú)"
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Seleccionar idioma", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column {
                    listOf(
                        "es" to "🇵🇪  Español",
                        "en" to "🇺🇸  English",
                        "pt" to "🇧🇷  Português"
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
                    Text("Cerrar", color = secondaryText)
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
                    text = "Términos y Condiciones",
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
                        """
Términos y Condiciones de Uso — HOMEFIX
Última actualización: 2026 | Aplicable en Lima Metropolitana, Perú

1. Aceptación
Al registrarte en HOMEFIX declaras ser mayor de 18 años y aceptar estos Términos y Condiciones, así como la Política de Privacidad. El uso continuo de la aplicación implica la aceptación de cualquier actualización futura.
2. Descripción del servicio
HOMEFIX es una plataforma digital de intermediación que conecta clientes con técnicos independientes de servicios del hogar en Lima Metropolitana. La aplicación no presta servicios técnicos directamente, no emplea a los técnicos registrados, no supervisa la ejecución del trabajo ni procesa pagos. Los acuerdos económicos y condiciones del servicio son responsabilidad exclusiva de las partes involucradas.
3. Registro y cuenta
El usuario debe proporcionar información verdadera, completa y actualizada al registrarse. Está prohibido crear cuentas falsas o duplicadas, suplantar identidades o utilizar la plataforma para fines ajenos a su objeto. El usuario es responsable de la confidencialidad de sus credenciales de acceso.
4. Datos personales y privacidad
HOMEFIX recopila únicamente los datos necesarios para el funcionamiento de la plataforma, incluyendo información de identificación, contacto, ubicación GPS, fotografías e historial de solicitudes. Estos datos se tratan conforme a la Ley N.° 29733 de Protección de Datos Personales del Perú. HOMEFIX no vende ni comparte datos personales con terceros salvo los servicios técnicos necesarios para su operación: Firebase Authentication y Firestore para autenticación y almacenamiento, Cloudinary para imágenes, Google Maps para geolocalización y WhatsApp como canal externo de comunicación entre usuarios.
5. Permisos del dispositivo
La aplicación solicitará permisos de cámara, galería, ubicación y notificaciones únicamente cuando sean necesarios para el funcionamiento de sus funcionalidades. Denegar alguno de estos permisos puede limitar ciertas funciones de la app.
6. Obligaciones de los usuarios
Los clientes se comprometen a publicar solicitudes reales, describir claramente el servicio y calificar de forma objetiva. Los técnicos se comprometen a declarar información veraz sobre su identidad y especialidades, mantener actualizada su disponibilidad y actuar con profesionalismo. Cualquier daño, incumplimiento o desacuerdo derivado de la prestación del servicio es responsabilidad exclusiva de las partes.
7. Seguridad
HOMEFIX implementa medidas técnicas razonables para proteger la información de los usuarios, incluyendo autenticación, control de acceso por rol y reglas de seguridad en base de datos. Sin embargo, ningún sistema conectado a internet garantiza seguridad absoluta. HOMEFIX no se responsabiliza por pérdida de credenciales, mal uso de la cuenta o ataques de terceros.
8. Calificaciones y sanciones
Las calificaciones deben basarse en experiencias reales y expresarse con respeto. HOMEFIX podrá retirar comentarios ofensivos, falsos o difamatorios, así como suspender o eliminar cuentas que incumplan estos términos, registren información falsa o hagan uso indebido de la plataforma.
9. Notificaciones
HOMEFIX enviará notificaciones relacionadas con el estado de las solicitudes, selección de técnicos, verificación de cuenta y otros eventos relevantes. El usuario puede gestionar sus preferencias de notificación desde la configuración de la aplicación. Desactivarlas puede afectar el seguimiento oportuno de los servicios.
10. Derechos del usuario
El usuario puede ejercer sus derechos de acceso, rectificación, cancelación y oposición sobre sus datos personales contactando al equipo de soporte de HOMEFIX. En caso de solicitar la eliminación de cuenta, los datos serán suprimidos salvo aquellos que deban conservarse por razones legales o de seguridad.
11. Modificaciones y legislación
HOMEFIX puede modificar estos términos por razones técnicas, legales u operativas, comunicándolo a través de la aplicación. Estos términos se rigen por las leyes de la República del Perú y su ámbito funcional se limita a Lima Metropolitana.
                    """.trimIndent()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTermsDialog = false
                    }
                ) {
                    Text("Cerrar")
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = textColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configuraciones", style = MaterialTheme.typography.headlineMedium, color = textColor, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Apariencia
            SettingsSectionTitle(title = "Apariencia", color = secondaryText)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                iconColor = if (darkMode) Color(0xFF90CAF9) else Warning,
                title = if (darkMode) "Modo oscuro" else "Modo claro",
                subtitle = if (darkMode) "Activado" else "Desactivado",
                checked = darkMode,
                onCheckedChange = { scope.launch { prefs.setDarkMode(it) } },
                titleColor = textColor,
                subtitleColor = secondaryText,
                cardColor = surfaceColor,
                borderColor = outlineColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsClickItem(
                icon = Icons.Default.Language,
                iconColor = Info,
                title = "Idioma",
                subtitle = languageLabel,
                onClick = { showLanguageDialog = true },
                titleColor = textColor,
                subtitleColor = secondaryText,
                cardColor = surfaceColor,
                borderColor = outlineColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Notificaciones
            SettingsSectionTitle(title = "Notificaciones", color = secondaryText)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = Icons.Default.VolumeUp,
                iconColor = Secondary,
                title = "Sonido",
                subtitle = if (notifSound) "Las notificaciones emiten sonido" else "Sin sonido",
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
                title = "Vibración",
                subtitle = if (notifVibration) "El dispositivo vibra" else "Sin vibración",
                checked = notifVibration,
                onCheckedChange = { scope.launch { prefs.setNotifVibration(it) } },
                titleColor = textColor,
                subtitleColor = secondaryText,
                cardColor = surfaceColor,
                borderColor = outlineColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Cuenta
            SettingsSectionTitle(title = "Cuenta", color = secondaryText)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsClickItem(
                icon = Icons.Default.Lock,
                iconColor = Primary,
                title = "Privacidad",
                subtitle = "Términos y condiciones",
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
                Text("Cerrar sesión", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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