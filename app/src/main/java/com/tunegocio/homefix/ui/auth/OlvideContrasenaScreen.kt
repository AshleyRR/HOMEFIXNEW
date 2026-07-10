package com.tunegocio.homefix.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.components.HomefixTextField
import com.tunegocio.homefix.ui.components.isValidEmail
import com.tunegocio.homefix.ui.theme.*

// NUEVO - MULTIDIOMA:
// Permite obtener los textos de strings_forgot_password.xml
// según el idioma activo de la aplicación.
import androidx.compose.ui.res.stringResource

// NUEVO - MULTIDIOMA:
// Permite acceder a las claves definidas en strings_forgot_password.xml.
import com.tunegocio.homefix.R

@Composable
fun OlvideContrasenaScreen(navController: NavController) {


    val auth = FirebaseAuth.getInstance()

    // NUEVO - MULTIDIOMA:
    // Estos mensajes se obtienen durante la composición con stringResource.
    // Luego pueden usarse de forma segura dentro de los callbacks de Firebase
    // sin consultar recursos desde addOnFailureListener.
    val forgotEmailRequired =
        stringResource(R.string.forgot_password_email_required)
    val forgotInvalidEmail =
        stringResource(R.string.forgot_password_invalid_email)
    val forgotEmailNotFound =
        stringResource(R.string.forgot_password_email_not_found)
    val forgotGenericSendError =
        stringResource(R.string.forgot_password_generic_send_error)

    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var emailEnviado by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }

    fun enviarRecuperacion() {
        emailError = ""
        mensajeError = ""

        if (email.isBlank()) {
            // MODIFICADO - MULTIDIOMA:
            // La validación conserva la misma condición.
            // Solo cambia el origen del mensaje visible.
            emailError = forgotEmailRequired
            return
        }

        if (!isValidEmail(email)) {
            // MODIFICADO - MULTIDIOMA:
            // La validación del correo no cambia.
            emailError = forgotInvalidEmail
            return
        }

        enviando = true

        // La llamada a Firebase Authentication permanece exactamente igual.
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                enviando = false
                emailEnviado = true
            }
            .addOnFailureListener { e ->
                enviando = false

                // MODIFICADO - MULTIDIOMA:
                // Se conservan las mismas condiciones para interpretar el error.
                // Únicamente se traducen los mensajes que verá el usuario.
                mensajeError = when {
                    e.message?.contains("no user") == true ->
                        forgotEmailNotFound

                    e.message?.contains("invalid") == true ->
                        forgotInvalidEmail

                    else ->
                        forgotGenericSendError
                }
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,

                        // MODIFICADO - MULTIDIOMA:
                        // Solo se traduce la descripción accesible.
                        // La acción del botón no cambia.
                        contentDescription = stringResource(
                            R.string.forgot_password_back
                        ),

                        tint = TextPrimary
                    )
                }

                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(
                        R.string.forgot_password_screen_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (emailEnviado) {
                // Pantalla de éxito
                Text(text = "✅", fontSize = 64.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(
                        R.string.forgot_password_email_sent_title
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(
                        R.string.forgot_password_email_sent_description
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        // El correo continúa mostrando el dato escrito por el usuario.
                        text = email,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                HomefixButton(
                    // MODIFICADO - MULTIDIOMA:
                    // El botón conserva la misma navegación.
                    text = stringResource(
                        R.string.forgot_password_back_to_login
                    ),
                    onClick = { navController.popBackStack() }
                )

            } else {
                // Formulario
                Text(text = "🔐", fontSize = 52.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(
                        R.string.forgot_password_question
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(
                        R.string.forgot_password_instructions
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(
                        R.string.forgot_password_requirements_reminder
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                HomefixTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (it.isNotBlank()) emailError = ""
                        mensajeError = ""
                    },

                    // MODIFICADO - MULTIDIOMA:
                    label = stringResource(
                        R.string.forgot_password_email_label
                    ),

                    isError = emailError.isNotEmpty(),
                    errorMessage = emailError,
                    keyboardType = KeyboardType.Email
                )

                if (mensajeError.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = mensajeError,
                        color = Error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                HomefixButton(
                    // MODIFICADO - MULTIDIOMA:
                    // La función enviarRecuperacion() permanece igual.
                    text = stringResource(
                        R.string.forgot_password_send_link
                    ),
                    onClick = { enviarRecuperacion() },
                    isLoading = enviando
                )
            }
        }
    }
}
