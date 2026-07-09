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

// NUEVO - MULTIDIOMA:
// Permite obtener los textos del archivo strings_login.xml según el idioma activo.
import androidx.compose.ui.res.stringResource
import com.tunegocio.homefix.R

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.tunegocio.homefix.data.local.database.LocalDatabase

import androidx.compose.ui.graphics.Color
@Composable
fun LoginScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // NUEVO - MULTIDIOMA:
    // Todos los textos se resuelven durante la composición mediante stringResource.
    // Luego pueden usarse de forma segura dentro de login(), Retrofit y Firebase
    // sin consultar recursos desde callbacks asíncronos.
    val loginEmailRequired = stringResource(R.string.login_email_required)
    val loginInvalidEmail = stringResource(R.string.login_invalid_email)
    val loginPasswordRequired = stringResource(R.string.login_password_required)
    val loginPasswordMinLength = stringResource(R.string.login_password_min_length)
    val loginVerifyEmail = stringResource(R.string.login_verify_email)
    val loginProfileError = stringResource(R.string.login_profile_error)
    val loginAuthenticationError = stringResource(R.string.login_authentication_error)
    val loginEmailNotFound = stringResource(R.string.login_email_not_found)
    val loginIncorrectPassword = stringResource(R.string.login_incorrect_password)
    val loginUserDisabled = stringResource(R.string.login_user_disabled)
    val loginInvalidCredentials = stringResource(R.string.login_invalid_credentials)
    val loginTooManyAttempts = stringResource(R.string.login_too_many_attempts)
    val loginGenericError = stringResource(R.string.login_generic_error)
    val loginUnknownError = stringResource(R.string.login_unknown_error)

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
            // MODIFICADO - MULTIDIOMA:
            // La validación es la misma; solo cambia el origen del mensaje.
            emailError = loginEmailRequired
            hasError = true
        } else if (!isValidEmail(email)) {
            // MODIFICADO - MULTIDIOMA:
            emailError = loginInvalidEmail
            hasError = true
        }

        if (password.isBlank()) {
            // MODIFICADO - MULTIDIOMA:
            passwordError = loginPasswordRequired
            hasError = true
        } else if (password.length < 6) {
            // MODIFICADO - MULTIDIOMA:
            passwordError = loginPasswordMinLength
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
                                    // MODIFICADO - MULTIDIOMA:
                                    // Mantiene la misma validación de email confirmado.
                                    generalError = loginVerifyEmail
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
                                        // MODIFICADO - MULTIDIOMA:
                                        generalError = loginProfileError
                                    }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                // MODIFICADO - MULTIDIOMA:
                                generalError = loginAuthenticationError
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
                        // MODIFICADO - MULTIDIOMA:
                        // Los códigos de Firebase no cambian. Solo los mensajes visibles
                        // se obtienen de strings_login.xml.
                        when (firebaseError?.error?.message) {
                            "EMAIL_NOT_FOUND" ->
                                emailError = loginEmailNotFound
                            "INVALID_PASSWORD" ->
                                passwordError = loginIncorrectPassword
                            "USER_DISABLED" ->
                                generalError = loginUserDisabled
                            "INVALID_LOGIN_CREDENTIALS" ->
                                generalError = loginInvalidCredentials
                            "TOO_MANY_ATTEMPTS_TRY_LATER" ->
                                generalError = loginTooManyAttempts
                            else ->
                                generalError = loginGenericError
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    //generalError = "Sin conexión a internet"
                    // MODIFICADO - MULTIDIOMA:
                    // Se conserva el mensaje técnico de la excepción cuando existe.
                    // Solo el texto de respaldo se obtiene del recurso traducido.
                    generalError = e.message ?: loginUnknownError
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
                // MODIFICADO - MULTIDIOMA:
                // Solo cambia el texto visible; la estructura de la pantalla no cambia.
                text = stringResource(R.string.login_welcome),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                // MODIFICADO - MULTIDIOMA:
                text = stringResource(R.string.login_subtitle),
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
                // MODIFICADO - MULTIDIOMA:
                label = stringResource(R.string.login_email_label),
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
                // MODIFICADO - MULTIDIOMA:
                label = stringResource(R.string.login_password_label),
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
                        // MODIFICADO - MULTIDIOMA:
                        // La navegación hacia recuperación de contraseña permanece igual.
                        text = stringResource(R.string.login_forgot_password),
                        color = Primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HomefixButton(
                // MODIFICADO - MULTIDIOMA:
                // El botón conserva login() y toda su lógica.
                text = stringResource(R.string.login_button),
                onClick = { login() },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.login_no_account),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    // La navegación hacia Register permanece igual.
                    text = stringResource(R.string.login_register),
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