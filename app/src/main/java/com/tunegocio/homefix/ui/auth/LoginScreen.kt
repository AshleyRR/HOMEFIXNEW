package com.tunegocio.homefix.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.components.HomefixTextField
import com.tunegocio.homefix.ui.components.isValidEmail
import com.tunegocio.homefix.ui.theme.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.tunegocio.homefix.R

import androidx.compose.ui.graphics.Color
@Composable
fun LoginScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var generalError by remember { mutableStateOf("") }

    fun login() {
        emailError = ""
        passwordError = ""
        generalError = ""

        var hasError = false

        if (email.isBlank()) {
            emailError = "El correo es obligatorio"
            hasError = true
        } else if (!isValidEmail(email)) {
            emailError = "Ingresa un correo válido (ej: correo@gmail.com)"
            hasError = true
        }

        if (password.isBlank()) {
            passwordError = "La contraseña es obligatoria"
            hasError = true
        } else if (password.length < 6) {
            passwordError = "Mínimo 6 caracteres"
            hasError = true
        }

        if (hasError) return

        isLoading = true

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // Verificar email confirmado antes de dejar entrar
                if (result.user?.isEmailVerified == false) {
                    isLoading = false
                    generalError = "Debes verificar tu email antes de ingresar"
                    auth.signOut()
                    return@addOnSuccessListener
                }

                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        isLoading = false
                        val role = doc.getString("role") ?: "client"
                        if (role == "technician") {
                            navController.navigate(Routes.HOME_TECHNICIAN) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.HOME_CLIENT) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                when {
                    e.message?.contains("password") == true ->
                        passwordError = "Contraseña incorrecta"
                    e.message?.contains("no user") == true ->
                        emailError = "No existe una cuenta con ese correo"
                    e.message?.contains("email") == true ->
                        emailError = "Correo inválido"
                    e.message?.contains("blocked") == true ->
                        generalError = "Demasiados intentos, intenta más tarde"
                    else ->
                        generalError = "Error al iniciar sesión, intenta de nuevo"
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Image(
                painter = painterResource(id = R.drawable.login_home),
                contentDescription = null,
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(80.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Bienvenido a HomeFix",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Inicia sesión para continuar",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            HomefixTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (it.isNotBlank()) emailError = ""
                },
                label = "Correo electrónico",
                isError = emailError.isNotEmpty(),
                errorMessage = emailError,
                keyboardType = KeyboardType.Email
            )
            Spacer(modifier = Modifier.height(12.dp))

            HomefixTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (it.isNotBlank()) passwordError = ""
                },
                label = "Contraseña",
                isPassword = true,
                isError = passwordError.isNotEmpty(),
                errorMessage = passwordError
            )

            if (generalError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = Error.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = generalError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Botón "¿Olvidaste tu contraseña?"
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { navController.navigate(Routes.OLVIDE_CONTRASENA) }
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = Primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HomefixButton(
                text = "Iniciar sesión",
                onClick = { login() },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text(
                    text = "¿No tienes cuenta? ",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Regístrate",
                    color = Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        navController.navigate(Routes.REGISTER)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}