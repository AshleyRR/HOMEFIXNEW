package com.tunegocio.homefix.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun VerificarEmailScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val usuario = auth.currentUser
    var enviando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf("") }
    var verificando by remember { mutableStateOf(false) }

    // Enviar email de verificación automáticamente al entrar
    LaunchedEffect(Unit) {
        try {
            usuario?.sendEmailVerification()
        } catch (e: Exception) {
            mensajeError = "No se pudo enviar el email"
        }
    }

    // Verificar cada 3 segundos si ya confirmó el email
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            usuario?.reload()?.addOnSuccessListener {
                if (auth.currentUser?.isEmailVerified == true) {
                    // Email verificado — ir al login
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.VERIFICAR_EMAIL) { inclusive = true }
                    }
                }
            }
        }
    }

    fun reenviarEmail() {
        enviando = true
        mensajeError = ""
        mensajeExito = ""
        usuario?.sendEmailVerification()
            ?.addOnSuccessListener {
                enviando = false
                mensajeExito = "Email reenviado correctamente"
            }
            ?.addOnFailureListener {
                enviando = false
                mensajeError = "Error al reenviar el email"
            }
    }

    fun verificarManualmente() {
        verificando = true
        usuario?.reload()?.addOnSuccessListener {
            verificando = false
            if (auth.currentUser?.isEmailVerified == true) {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.VERIFICAR_EMAIL) { inclusive = true }
                }
            } else {
                mensajeError = "Aún no has verificado tu email"
            }
        }?.addOnFailureListener {
            verificando = false
            mensajeError = "Error al verificar, intenta de nuevo"
        }
    }

    fun cancelarRegistro() {
        auth.currentUser?.delete()
        auth.signOut()
        navController.navigate(Routes.REGISTER) {
            popUpTo(Routes.VERIFICAR_EMAIL) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "📧", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verifica tu email",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Te enviamos un correo de verificación a:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Primary.copy(alpha = 0.08f)
            ) {
                Text(
                    text = usuario?.email ?: "",
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

            Spacer(modifier = Modifier.height(20.dp))

            // Pasos para el usuario
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PasoVerificacion(
                        numero = "1",
                        texto = "Abre tu correo electrónico"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PasoVerificacion(
                        numero = "2",
                        texto = "Busca el email de HomeFix"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PasoVerificacion(
                        numero = "3",
                        texto = "Haz clic en \"Verificar email\""
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PasoVerificacion(
                        numero = "4",
                        texto = "Vuelve aquí y toca el botón de abajo"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mensajes
            if (mensajeExito.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Success.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = mensajeExito,
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        ),
                        color = Success,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (mensajeError.isNotEmpty()) {
                Text(
                    text = mensajeError,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Botón principal
            HomefixButton(
                text = "Ya verifiqué mi email ✓",
                onClick = { verificarManualmente() },
                isLoading = verificando
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reenviar email
            OutlinedButton(
                onClick = { reenviarEmail() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !enviando
            ) {
                if (enviando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Primary
                    )
                } else {
                    Text(
                        "Reenviar email",
                        color = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancelar registro
            TextButton(onClick = { cancelarRegistro() }) {
                Text(
                    text = "Cancelar registro",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun PasoVerificacion(numero: String, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(26.dp),
            shape = RoundedCornerShape(13.dp),
            color = Primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = numero,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}