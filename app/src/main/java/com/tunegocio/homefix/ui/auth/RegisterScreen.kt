package com.tunegocio.homefix.ui.auth

import com.tunegocio.homefix.data.remote.RetrofitClient
import com.tunegocio.homefix.data.remote.models.RegisterRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext






import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.ALL_SPECIALTIES
import com.tunegocio.homefix.data.CloudinaryUploader
import com.tunegocio.homefix.data.MAX_SPECIALTIES
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.components.HomefixTextField
import com.tunegocio.homefix.ui.components.PasswordRequirements
import com.tunegocio.homefix.ui.components.isValidEmail
import com.tunegocio.homefix.ui.components.validatePassword
import com.tunegocio.homefix.ui.theme.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.tunegocio.homefix.R


val LIMA_DISTRICTS = listOf(
    "Ancón", "Ate", "Barranco", "Breña", "Carabayllo",
    "Cercado de Lima", "Chaclacayo", "Chorrillos", "Cieneguilla",
    "Comas", "El Agustino", "Independencia", "Jesús María",
    "La Molina", "La Victoria", "Lince", "Los Olivos",
    "Lurigancho", "Lurín", "Magdalena del Mar", "Miraflores",
    "Pachacámac", "Pucusana", "Pueblo Libre", "Puente Piedra",
    "Punta Hermosa", "Punta Negra", "Rímac", "San Bartolo",
    "San Borja", "San Isidro", "San Juan de Lurigancho",
    "San Juan de Miraflores", "San Luis", "San Martín de Porres",
    "San Miguel", "Santa Anita", "Santa María del Mar",
    "Santa Rosa", "Santiago de Surco", "Surquillo",
    "Villa El Salvador", "Villa María del Triunfo"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Campos comunes
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf("") }
    var showDistrictDialog by remember { mutableStateOf(false) }

    // Selfie — ambos roles
    var selfieUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Campos solo técnico
    var dni by remember { mutableStateOf("") }
    var yearsExp by remember { mutableStateOf("") }
    var selectedSpecialties by remember { mutableStateOf(listOf<String>()) }
    var showSpecialtyDialog by remember { mutableStateOf(false) }

    // Términos y mayoría de edad
    var aceptoTerminos by remember { mutableStateOf(false) }
    var esMayorDeEdad by remember { mutableStateOf(false) }
    var mostrarTerminos by remember { mutableStateOf(false) }

    // Estado general
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Errores por campo
    var firstNameError by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    var districtError by remember { mutableStateOf("") }
    var selfieError by remember { mutableStateOf("") }
    var roleError by remember { mutableStateOf("") }
    var dniError by remember { mutableStateOf("") }
    var specialtyError by remember { mutableStateOf("") }
    var terminosError by remember { mutableStateOf("") }
    var edadError by remember { mutableStateOf("") }

    // Archivo temporal para selfie
    val selfieFile = remember { File(context.cacheDir, "selfie_${UUID.randomUUID()}.jpg") }
    val selfieUriForCamera = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", selfieFile)
    }

    // Launcher cámara
    val selfieLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selfieUri = selfieUriForCamera
            selfieError = ""
        }
    }

    // Launcher permiso cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) selfieLauncher.launch(selfieUriForCamera)
    }

    // Diálogo distrito
    if (showDistrictDialog) {
        DistrictPickerDialog(
            districts = LIMA_DISTRICTS,
            selectedDistrict = selectedDistrict,
            onDismiss = { showDistrictDialog = false },
            onConfirm = { district ->
                selectedDistrict = district
                districtError = ""
                showDistrictDialog = false
            }
        )
    }

    // Diálogo especialidades
    if (showSpecialtyDialog) {
        SpecialtyPickerDialog(
            allSpecialties = ALL_SPECIALTIES,
            selectedSpecialties = selectedSpecialties,
            maxSelection = MAX_SPECIALTIES,
            onDismiss = { showSpecialtyDialog = false },
            onConfirm = { selected ->
                selectedSpecialties = selected
                specialtyError = ""
                showSpecialtyDialog = false
            }
        )
    }

    // Diálogo términos y condiciones
    if (mostrarTerminos) {
        DialogoTerminos(
            onAceptar = {
                aceptoTerminos = true
                terminosError = ""
                mostrarTerminos = false
            },
            onCerrar = { mostrarTerminos = false }
        )
    }

    fun register() {
        // Limpiar errores
        firstNameError = ""; lastNameError = ""; emailError = ""
        passwordError = ""; confirmPasswordError = ""; phoneError = ""
        districtError = ""; selfieError = ""; roleError = ""
        dniError = ""; specialtyError = ""; errorMessage = ""
        terminosError = ""; edadError = ""

        var hasError = false

        if (firstName.isBlank()) {
            firstNameError = "Los nombres son obligatorios"
            hasError = true
        } else if (firstName.trim().length < 2) {
            firstNameError = "Mínimo 2 caracteres"
            hasError = true
        }

        if (lastName.isBlank()) {
            lastNameError = "Los apellidos son obligatorios"
            hasError = true
        } else if (lastName.trim().length < 2) {
            lastNameError = "Mínimo 2 caracteres"
            hasError = true
        }

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
        } else {
            val validation = validatePassword(password)
            if (!validation.hasMinLength) {
                passwordError = "Mínimo 6 caracteres"
                hasError = true
            } else if (!validation.hasUppercase) {
                passwordError = "Debe tener al menos una mayúscula"
                hasError = true
            } else if (!validation.hasNumber) {
                passwordError = "Debe tener al menos un número"
                hasError = true
            }
        }

        if (confirmPassword.isBlank()) {
            confirmPasswordError = "Confirma tu contraseña"
            hasError = true
        } else if (password != confirmPassword) {
            confirmPasswordError = "Las contraseñas no coinciden"
            hasError = true
        }

        if (phone.isBlank()) {
            phoneError = "El teléfono es obligatorio"
            hasError = true
        } else if (phone.length < 9) {
            phoneError = "Ingresa un número válido de 9 dígitos"
            hasError = true
        }

        if (selectedDistrict.isEmpty()) {
            districtError = "Selecciona tu distrito"
            hasError = true
        }

        if (selectedRole.isEmpty()) {
            roleError = "Selecciona cómo quieres usar HomeFix"
            hasError = true
        }

        if (selfieUri == null) {
            selfieError = "La foto de perfil es obligatoria"
            hasError = true
        }

        if (selectedRole == "technician") {
            if (dni.isBlank()) {
                dniError = "El DNI es obligatorio"
                hasError = true
            } else if (dni.length != 8) {
                dniError = "El DNI debe tener exactamente 8 dígitos"
                hasError = true
            }
            if (selectedSpecialties.isEmpty()) {
                specialtyError = "Selecciona al menos una especialidad"
                hasError = true
            }
        }

        if (!aceptoTerminos) {
            terminosError = "Debes aceptar los términos y condiciones"
            hasError = true
        }

        if (!esMayorDeEdad) {
            edadError = "Debes ser mayor de 18 años para registrarte"
            hasError = true
        }

        if (hasError) return
        isLoading = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.register(
                    url = RetrofitClient.REGISTER_URL,
                    apiKey = RetrofitClient.API_KEY,
                    request = RegisterRequest(
                        email = email.trim(),
                        password = password
                    )
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val registerData = response.body()
                        val uid = registerData?.localId ?: ""

                        // Autenticar en Firebase SDK
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { authResult ->

                                fun saveUser(selfieUrl: String) {
                                    val user = UserModel(
                                        uid = uid,
                                        name = firstName.trim(),
                                        lastName = lastName.trim(),
                                        email = email.trim(),
                                        role = selectedRole,
                                        phone = phone.trim(),
                                        district = selectedDistrict,
                                        selfieUrl = selfieUrl,
                                        dni = if (selectedRole == "technician") dni.trim() else "",
                                        yearsExp = yearsExp.toIntOrNull() ?: 0,
                                        specialties = selectedSpecialties,
                                        whatsapp = phone.trim(),
                                        createdAt = System.currentTimeMillis()
                                    )
                                    db.collection("users").document(uid).set(user)
                                        .addOnSuccessListener {
                                            authResult.user?.sendEmailVerification()
                                            isLoading = false
                                            navController.navigate(Routes.VERIFICAR_EMAIL) {
                                                popUpTo(Routes.REGISTER) { inclusive = true }
                                            }
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            errorMessage = "Error al guardar datos"
                                        }
                                }

                                if (selfieUri != null) {
                                    scope.launch {
                                        val uploadResult = CloudinaryUploader.uploadImage(
                                            context = context,
                                            uri = selfieUri!!,
                                            folder = "homefix/selfies"
                                        )
                                        uploadResult.fold(
                                            onSuccess = { url: String -> saveUser(url) },
                                            onFailure = { _: Throwable ->
                                                isLoading = false
                                                errorMessage = "Error al subir la foto"
                                            }
                                        )
                                    }
                                } else {
                                    saveUser("")
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                errorMessage = "Error al autenticar tras registro"
                            }

                    } else {
                        isLoading = false
                        when {
                            response.code() == 400 ->
                                emailError = "Ese correo ya está registrado"
                            else ->
                                errorMessage = "Error al registrarse, intenta de nuevo"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = e.message ?: "Error al registrarse"
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
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Crear cuenta",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Completa tus datos para comenzar",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selección de rol
            Text(
                text = "¿Cómo quieres usar HomeFix?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleCard(
                    title = "Cliente",
                    description = "Necesito un técnico",
                    emoji = "",
                    isSelected = selectedRole == "client",
                    color = ClientColor,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = "client"; roleError = "" }
                )
                RoleCard(
                    title = "Técnico",
                    description = "Ofrezco servicios",
                    emoji = "",
                    isSelected = selectedRole == "technician",
                    color = TechnicianColor,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = "technician"; roleError = "" }
                )
            }
            if (roleError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = roleError,
                    color = Error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nombres y Apellidos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomefixTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        if (it.isNotBlank()) firstNameError = ""
                    },
                    label = "Nombres",
                    modifier = Modifier.weight(1f),
                    isError = firstNameError.isNotEmpty(),
                    errorMessage = firstNameError
                )
                HomefixTextField(
                    value = lastName,
                    onValueChange = {
                        lastName = it
                        if (it.isNotBlank()) lastNameError = ""
                    },
                    label = "Apellidos",
                    modifier = Modifier.weight(1f),
                    isError = lastNameError.isNotEmpty(),
                    errorMessage = lastNameError
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Email
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

            // Contraseña
            HomefixTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (it.isNotBlank()) passwordError = ""
                    confirmPasswordError = ""
                },
                label = "Contraseña",
                isPassword = true,
                isError = passwordError.isNotEmpty(),
                errorMessage = passwordError
            )
            PasswordRequirements(password = password)
            Spacer(modifier = Modifier.height(12.dp))

            // Confirmar contraseña
            HomefixTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (it.isNotBlank()) confirmPasswordError = ""
                },
                label = "Confirmar contraseña",
                isPassword = true,
                isError = confirmPasswordError.isNotEmpty(),
                errorMessage = confirmPasswordError
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Teléfono
            HomefixTextField(
                value = phone,
                onValueChange = {
                    if (it.all { char -> char.isDigit() } && it.length <= 9) {
                        phone = it
                        if (it.isNotBlank()) phoneError = ""
                    }
                },
                label = "Número de teléfono",
                isError = phoneError.isNotEmpty(),
                errorMessage = phoneError,
                keyboardType = KeyboardType.Number
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Distrito
            Text(
                text = "Distrito",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = { showDistrictDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (districtError.isNotEmpty()) Error else Primary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (districtError.isNotEmpty()) Error else CardBorder
                )
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedDistrict.isEmpty()) "Selecciona tu distrito" else selectedDistrict,
                    fontWeight = if (selectedDistrict.isNotEmpty()) FontWeight.Medium else FontWeight.Normal
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (districtError.isNotEmpty()) {
                Text(
                    text = districtError,
                    color = Error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Foto de perfil
            HorizontalDivider(color = CardBorder)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Foto de perfil",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Tómate una selfie en este momento. Es obligatoria para todos los usuarios.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (selfieUri != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = selfieUri,
                        contentDescription = "Selfie",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(55.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-80).dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Error
                    ) {
                        IconButton(
                            onClick = { selfieUri = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Repetir selfie",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Toca la X para tomar otra foto",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                OutlinedButton(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selfieError.isNotEmpty()) Error else Primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selfieError.isNotEmpty()) Error else CardBorder
                    )
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tomar selfie ahora")
                }
                if (selfieError.isNotEmpty()) {
                    Text(
                        text = selfieError,
                        color = Error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Campos solo para técnico
            if (selectedRole == "technician") {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Datos del técnico",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // DNI
                HomefixTextField(
                    value = dni,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 8) {
                            dni = it
                            if (it.isNotBlank()) dniError = ""
                        }
                    },
                    label = "DNI (8 dígitos)",
                    isError = dniError.isNotEmpty(),
                    errorMessage = dniError,
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Años de experiencia
                HomefixTextField(
                    value = yearsExp,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 2) {
                            yearsExp = it
                        }
                    },
                    label = "Años de experiencia",
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Especialidades
                Text(
                    text = "Especialidades (máximo $MAX_SPECIALTIES)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { showSpecialtyDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (specialtyError.isNotEmpty()) Error else Primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (specialtyError.isNotEmpty()) Error else CardBorder
                    )
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedSpecialties.isEmpty())
                            "Seleccionar especialidades"
                        else
                            "${selectedSpecialties.size} seleccionada${if (selectedSpecialties.size > 1) "s" else ""}",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (specialtyError.isNotEmpty()) {
                    Text(
                        text = specialtyError,
                        color = Error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 4.dp)
                    )
                }

                // Cards de especialidades seleccionadas
                if (selectedSpecialties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selectedSpecialties.forEach { specialty ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TechnicianColor.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = specialty,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TechnicianColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Quitar",
                                        tint = TechnicianColor,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable {
                                                selectedSpecialties = selectedSpecialties - specialty
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error general
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            // Términos y mayoría de edad
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox — Términos y condiciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = aceptoTerminos,
                    onCheckedChange = {
                        aceptoTerminos = it
                        if (it) terminosError = ""
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Primary,
                        uncheckedColor = if (terminosError.isNotEmpty()) Error else TextSecondary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row {
                        Text(
                            text = "He leído y acepto los ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Términos y Condiciones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { mostrarTerminos = true }
                        )
                    }
                    if (terminosError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = terminosError,
                            color = Error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Checkbox — Mayor de edad
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = esMayorDeEdad,
                    onCheckedChange = {
                        esMayorDeEdad = it
                        if (it) edadError = ""
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Primary,
                        uncheckedColor = if (edadError.isNotEmpty()) Error else TextSecondary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Confirmo que soy mayor de 18 años",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (edadError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = edadError,
                            color = Error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HomefixButton(
                text = "Crear cuenta",
                onClick = { register() },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text(
                    text = "¿Ya tienes cuenta? ",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Inicia sesión",
                    color = Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        navController.navigate(Routes.LOGIN)
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DistrictPickerDialog(
    districts: List<String>,
    selectedDistrict: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = districts.filter { it.contains(searchQuery, ignoreCase = true) }

    // ← Colores dinámicos
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Selecciona tu distrito",
                    fontWeight = FontWeight.Bold,
                    color = onSurface // ← antes sin color (heredaba negro)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar distrito...", color = onSurface.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = onSurface // ← ícono blanco en dark mode
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = outline,
                        focusedTextColor = onSurface,
                        unfocusedTextColor = onSurface,
                        focusedLeadingIconColor = onSurface,
                        unfocusedLeadingIconColor = onSurface
                    )
                )
            }
        },
        text = {
            Column {
                if (filtered.isEmpty()) {
                    Text(
                        text = "No se encontró ese distrito",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    filtered.forEach { district ->
                        val isSelected = district == selectedDistrict
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConfirm(district) }
                                .background(
                                    if (isSelected) Primary.copy(alpha = 0.08f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = district,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Primary else onSurface, // ← era TextPrimary (hardcodeado)
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = onSurface.copy(alpha = 0.5f))
            }
        }
    )
}
@Composable
fun SpecialtyPickerDialog(
    allSpecialties: List<String>,
    selectedSpecialties: List<String>,
    maxSelection: Int,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedSpecialties) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Selecciona tus especialidades",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Máximo $maxSelection — ${tempSelected.size}/$maxSelection seleccionadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tempSelected.size == maxSelection) Error else TextSecondary
                )
            }
        },
        text = {
            Column {
                allSpecialties.forEach { specialty ->
                    val isSelected = tempSelected.contains(specialty)
                    val isDisabled = !isSelected && tempSelected.size >= maxSelection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDisabled) {
                                tempSelected = if (isSelected) {
                                    tempSelected - specialty
                                } else {
                                    tempSelected + specialty
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            enabled = !isDisabled,
                            colors = CheckboxDefaults.colors(checkedColor = TechnicianColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = specialty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDisabled) TextHint else TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(tempSelected) },
                enabled = tempSelected.isNotEmpty()
            ) {
                Text(
                    "Confirmar",
                    color = if (tempSelected.isNotEmpty()) Primary else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    emoji: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.1f) else CardBackground
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, color)
        else
            androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(
                    id = if (title == "Cliente") R.drawable.registro_cliente else R.drawable.registro_tecnico
                ),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Fit
            )


            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) color else TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}