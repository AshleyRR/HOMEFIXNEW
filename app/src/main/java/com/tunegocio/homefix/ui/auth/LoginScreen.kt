package com.tunegocio.homefix.ui.auth

//imports para el retrofit con link firebase
import com.tunegocio.homefix.data.remote.RetrofitClient
import com.tunegocio.homefix.data.remote.models.LoginRequest
import com.tunegocio.homefix.data.remote.models.FirebaseErrorResponse
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.tunegocio.homefix.data.local.database.LocalDatabase

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

    val context = LocalContext.current
    val localDb = LocalDatabase(context)


    // Forzar apertura de la BD para que aparezca en Database Inspector
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            localDb.obtenerConfiguracion()
        }
    }

    // FUNCION LOGIN ACTUALIZADA CON RETROFIT LINK FIREBASE

    //------------------------------------------------------

    fun login() {
        emailError = ""
        passwordError = ""
        generalError = ""

        var hasError = false

        if (email.isBlank()) {
            emailError = "El correo es obligatorio"
            hasError = true
        } else if (!isValidEmail(email)) {
            emailError = "Ingresa un correo válido"
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

        // Llamada a Firebase REST API via Retrofit
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.login(
                    url = RetrofitClient.LOGIN_URL,
                    apiKey = RetrofitClient.API_KEY,
                    request = LoginRequest(
                        email = email.trim(),
                        password = password
                    )
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val loginData = response.body()
                        val uid = loginData?.localId ?: ""

                        // Autenticar en Firebase SDK para que currentUser no sea null
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { result ->

                                // Verificar email confirmado
                                if (result.user?.isEmailVerified == false) {
                                    isLoading = false
                                    generalError = "Debes verificar tu email antes de ingresar"
                                    auth.signOut()
                                    return@addOnSuccessListener
                                }

                                // Obtener perfil desde Firestore
                                db.collection("users").document(uid).get()
                                    .addOnSuccessListener { doc ->
                                        isLoading = false
                                        val role = doc.getString("role") ?: "client"

                                        // Guardar perfil en SQLite
                                        localDb.guardarUsuario(
                                            uid = uid,
                                            nombres = doc.getString("name") ?: "",
                                            apellidos = doc.getString("lastName") ?: "",
                                            email = doc.getString("email") ?: "",
                                            telefono = doc.getString("phone") ?: "",
                                            distrito = doc.getString("district") ?: "",
                                            rol = role,
                                            selfieUrl = doc.getString("selfieUrl") ?: "",
                                            calificacion = (doc.getDouble("rating") ?: 0.0).toFloat(),
                                            especialidades = (doc.get("specialties") as? List<*>)?.joinToString(",") ?: "",
                                            anosExperiencia = (doc.getLong("yearsExp") ?: 0L).toInt(),
                                            bio = doc.getString("bio") ?: "",
                                            lat = doc.getDouble("lat") ?: 0.0,
                                            lng = doc.getDouble("lng") ?: 0.0
                                        )

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
                                    .addOnFailureListener {
                                        isLoading = false
                                        generalError = "Error al obtener perfil"
                                    }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                generalError = "Error al autenticar"
                            }

                    } else {
                        // Parsear error de Firebase
                        val errorBody = response.errorBody()?.string()
                        val firebaseError = try {
                            Gson().fromJson(errorBody, FirebaseErrorResponse::class.java)
                        } catch (e: Exception) {
                            null
                        }

                        isLoading = false
                        when (firebaseError?.error?.message) {
                            "EMAIL_NOT_FOUND" ->
                                emailError = "No existe una cuenta con ese correo"
                            "INVALID_PASSWORD" ->
                                passwordError = "Contraseña incorrecta"
                            "USER_DISABLED" ->
                                generalError = "Esta cuenta ha sido deshabilitada"
                            "INVALID_LOGIN_CREDENTIALS" ->
                                generalError = "Credenciales inválidas"
                            "TOO_MANY_ATTEMPTS_TRY_LATER" ->
                                generalError = "Demasiados intentos, intenta más tarde"
                            else ->
                                generalError = "Error al iniciar sesión"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    //generalError = "Sin conexión a internet"
                    generalError = e.message ?: "Error desconocido"  // ← cambia esto temporalmente
                }

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
                color = MaterialTheme.colorScheme.onBackground,
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

            // Botón ¿Olvidaste tu contraseña?
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