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

@Composable
fun OlvideContrasenaScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var emailEnviado by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }

    fun enviarRecuperacion() {
        emailError = ""
        mensajeError = ""

        if (email.isBlank()) {
            emailError = "Ingresa tu correo"
            return
        }
        if (!isValidEmail(email)) {
            emailError = "Ingresa un correo válido"
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
                mensajeError = when {
                    e.message?.contains("no user") == true ->
                        "No existe una cuenta con ese correo"
                    e.message?.contains("invalid") == true ->
                        "Correo inválido"
                    else -> "Error al enviar, intenta de nuevo"
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
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
                        contentDescription = "Volver",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Recuperar contraseña",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (emailEnviado) {
                // Pantalla de éxito
                Text(text = "✅", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¡Email enviado!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Revisa tu correo y sigue las instrucciones para cambiar tu contraseña.",
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
                    text = "Volver al inicio de sesión",
                    onClick = { navController.popBackStack() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        emailEnviado = false
                        email = ""
                    }
                ) {
                    Text(
                        "Reenviar a otro correo",
                        color = TextSecondary
                    )
                }

            } else {
                // Formulario
                Text(text = "🔐", fontSize = 52.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ingresa tu correo y te enviaremos un link para crear una nueva contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))


                Text(
                    text = "Recuerda: Debe tener mínimo 6 caracteres, 1 mayúscula y 1 número.",
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
                    label = "Correo electrónico",
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
                    text = "Enviar link de recuperación",
                    onClick = { enviarRecuperacion() },
                    isLoading = enviando
                )
            }
        }
    }
}