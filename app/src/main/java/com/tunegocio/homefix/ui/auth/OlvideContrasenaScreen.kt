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
import androidx.compose.ui.res.stringResource
import com.tunegocio.homefix.R

/**
 * Pantalla de "Olvidé mi contraseña".
 * Permite al usuario ingresar su correo, validarlo y solicitar
 * a Firebase Authentication el envío de un enlace de recuperación.
 * Muestra un formulario inicial y, tras el envío exitoso, una vista de confirmación.
 */
@Composable
fun OlvideContrasenaScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()

    // Textos localizados (se leen aquí para poder usarlos dentro de callbacks de Firebase)
    val forgotEmailRequired = stringResource(R.string.forgot_password_email_required)
    val forgotInvalidEmail = stringResource(R.string.forgot_password_invalid_email)
    val forgotEmailNotFound = stringResource(R.string.forgot_password_email_not_found)
    val forgotGenericSendError = stringResource(R.string.forgot_password_generic_send_error)

    // Estado del formulario
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }          // error de validación local
    var enviando by remember { mutableStateOf(false) }         // controla el loading del botón
    var emailEnviado by remember { mutableStateOf(false) }     // true = mostrar vista de éxito
    var mensajeError by remember { mutableStateOf("") }        // error devuelto por Firebase

    // Valida el email y dispara el envío del correo de recuperación vía Firebase
    fun enviarRecuperacion() {
        emailError = ""
        mensajeError = ""

        if (email.isBlank()) {
            emailError = forgotEmailRequired
            return
        }

        if (!isValidEmail(email)) {
            emailError = forgotInvalidEmail
            return
        }

        enviando = true

        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                enviando = false
                emailEnviado = true
            }
            .addOnFailureListener { e ->
                enviando = false
                // Mapea el error de Firebase a un mensaje localizado
                mensajeError = when {
                    e.message?.contains("no user") == true -> forgotEmailNotFound
                    e.message?.contains("invalid") == true -> forgotInvalidEmail
                    else -> forgotGenericSendError
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

            // Barra superior: botón de retroceso + título
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.forgot_password_back),
                        tint = TextPrimary
                    )
                }

                Text(
                    text = stringResource(R.string.forgot_password_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Alterna entre formulario y vista de confirmación según emailEnviado
            if (emailEnviado) {
                // --- Vista de éxito ---
                Text(text = "✅", fontSize = 64.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.forgot_password_email_sent_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.forgot_password_email_sent_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chip con el correo al que se envió el enlace
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = email,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                HomefixButton(
                    text = stringResource(R.string.forgot_password_back_to_login),
                    onClick = { navController.popBackStack() }
                )

            } else {
                // --- Formulario ---
                Text(text = "🔐", fontSize = 52.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.forgot_password_question),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.forgot_password_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.forgot_password_requirements_reminder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Campo de correo (limpia errores previos al escribir)
                HomefixTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (it.isNotBlank()) emailError = ""
                        mensajeError = ""
                    },
                    label = stringResource(R.string.forgot_password_email_label),
                    isError = emailError.isNotEmpty(),
                    errorMessage = emailError,
                    keyboardType = KeyboardType.Email
                )

                // Error devuelto por Firebase (distinto del error de validación local)
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
                    text = stringResource(R.string.forgot_password_send_link),
                    onClick = { enviarRecuperacion() },
                    isLoading = enviando
                )
            }
        }
    }
}