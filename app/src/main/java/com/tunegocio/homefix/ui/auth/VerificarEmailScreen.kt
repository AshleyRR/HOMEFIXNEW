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

// NUEVO - MULTIDIOMA:
// Permite obtener los textos de strings_verify_email.xml
// según el idioma activo de la aplicación.
import androidx.compose.ui.res.stringResource

// NUEVO - MULTIDIOMA:
// Permite acceder a las claves definidas en strings_verify_email.xml.
import com.tunegocio.homefix.R

@Composable
fun VerificarEmailScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val usuario = auth.currentUser

    // NUEVO - MULTIDIOMA:
    // Estos mensajes se obtienen durante la composición con stringResource.
    // Luego pueden usarse de forma segura dentro de LaunchedEffect y callbacks
    // de Firebase sin consultar recursos desde procesos asíncronos.
    val verifyEmailCouldNotSend =
        stringResource(R.string.verify_email_could_not_send)

    val verifyEmailResentSuccess =
        stringResource(R.string.verify_email_resent_success)

    val verifyEmailResendError =
        stringResource(R.string.verify_email_resend_error)

    val verifyEmailNotVerifiedYet =
        stringResource(R.string.verify_email_not_verified_yet)

    val verifyEmailVerificationError =
        stringResource(R.string.verify_email_verification_error)

    var enviando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf("") }
    var verificando by remember { mutableStateOf(false) }

    // Enviar email de verificación automáticamente al entrar
    LaunchedEffect(Unit) {
        try {
            // La operación de Firebase permanece exactamente igual.
            usuario?.sendEmailVerification()
        } catch (e: Exception) {
            // MODIFICADO - MULTIDIOMA:
            // Solo se cambia el origen del mensaje visible.
            mensajeError = verifyEmailCouldNotSend
        }
    }

    // Verificar cada 3 segundos si ya confirmó el email
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)

            // La recarga del usuario y la navegación no cambian.
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

        // La función sendEmailVerification() mantiene el mismo comportamiento.
        usuario?.sendEmailVerification()
            ?.addOnSuccessListener {
                enviando = false

                // MODIFICADO - MULTIDIOMA:
                mensajeExito = verifyEmailResentSuccess
            }
            ?.addOnFailureListener {
                enviando = false

                // MODIFICADO - MULTIDIOMA:
                mensajeError = verifyEmailResendError
            }
    }

    fun verificarManualmente() {
        verificando = true

        // La verificación manual con Firebase permanece igual.
        usuario?.reload()?.addOnSuccessListener {
            verificando = false

            if (auth.currentUser?.isEmailVerified == true) {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.VERIFICAR_EMAIL) { inclusive = true }
                }
            } else {
                // MODIFICADO - MULTIDIOMA:
                mensajeError = verifyEmailNotVerifiedYet
            }
        }?.addOnFailureListener {
            verificando = false

            // MODIFICADO - MULTIDIOMA:
            mensajeError = verifyEmailVerificationError
        }
    }

    fun cancelarRegistro() {
        // La eliminación del usuario, cierre de sesión y navegación no cambian.
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
                // MODIFICADO - MULTIDIOMA:
                text = stringResource(R.string.verify_email_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                // MODIFICADO - MULTIDIOMA:
                text = stringResource(R.string.verify_email_sent_to),
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
                    // El correo continúa mostrando el dato real del usuario.
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PasoVerificacion(
                        numero = "1",

                        // MODIFICADO - MULTIDIOMA:
                        texto = stringResource(
                            R.string.verify_email_step_open_email
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PasoVerificacion(
                        numero = "2",
                        texto = stringResource(
                            R.string.verify_email_step_find_homefix_email
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PasoVerificacion(
                        numero = "3",
                        texto = stringResource(
                            R.string.verify_email_step_click_verify
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PasoVerificacion(
                        numero = "4",
                        texto = stringResource(
                            R.string.verify_email_step_return_and_tap
                        )
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
                // MODIFICADO - MULTIDIOMA:
                // La función verificarManualmente() permanece igual.
                text = stringResource(
                    R.string.verify_email_already_verified_button
                ),
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
                        // MODIFICADO - MULTIDIOMA:
                        text = stringResource(
                            R.string.verify_email_resend_button
                        ),
                        color = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancelar registro
            TextButton(onClick = { cancelarRegistro() }) {
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    // La acción cancelarRegistro() no cambia.
                    text = stringResource(
                        R.string.verify_email_cancel_registration
                    ),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun PasoVerificacion(
    numero: String,
    texto: String
) {
    // Este componente no necesita stringResource directamente,
    // porque recibe el texto ya traducido desde VerificarEmailScreen.
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
