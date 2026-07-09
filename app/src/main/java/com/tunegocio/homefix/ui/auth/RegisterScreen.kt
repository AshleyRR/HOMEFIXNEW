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

// NUEVO - MULTIDIOMA:
// Permite obtener los textos de strings_register.xml según el idioma activo.
import androidx.compose.ui.res.stringResource
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

    // NUEVO - MULTIDIOMA:
    // Estos mensajes se resuelven durante la composición y luego se reutilizan
    // dentro de register(), Retrofit, Firebase y Cloudinary. De esta forma no se
    // consulta LocalContext.current desde callbacks asíncronos.
    val registerFirstNameRequired = stringResource(R.string.register_first_name_required)
    val registerMinimumTwoCharacters = stringResource(R.string.register_minimum_two_characters)
    val registerLastNameRequired = stringResource(R.string.register_last_name_required)
    val registerEmailRequired = stringResource(R.string.register_email_required)
    val registerInvalidEmail = stringResource(R.string.register_invalid_email)
    val registerPasswordRequired = stringResource(R.string.register_password_required)
    val registerPasswordMinLength = stringResource(R.string.register_password_min_length)
    val registerPasswordUppercase = stringResource(R.string.register_password_uppercase)
    val registerPasswordNumber = stringResource(R.string.register_password_number)
    val registerConfirmPasswordRequired = stringResource(R.string.register_confirm_password_required)
    val registerPasswordsDoNotMatch = stringResource(R.string.register_passwords_do_not_match)
    val registerPhoneRequired = stringResource(R.string.register_phone_required)
    val registerInvalidPhone = stringResource(R.string.register_invalid_phone)
    val registerDistrictRequired = stringResource(R.string.register_district_required)
    val registerRoleRequired = stringResource(R.string.register_role_required)
    val registerPhotoRequired = stringResource(R.string.register_photo_required)
    val registerDniRequired = stringResource(R.string.register_dni_required)
    val registerInvalidDni = stringResource(R.string.register_invalid_dni)
    val registerSpecialtyRequired = stringResource(R.string.register_specialty_required)
    val registerTermsRequired = stringResource(R.string.register_terms_required)
    val registerAdultRequired = stringResource(R.string.register_adult_required)
    val registerSaveDataError = stringResource(R.string.register_save_data_error)
    val registerPhotoUploadError = stringResource(R.string.register_photo_upload_error)
    val registerAuthenticationError = stringResource(R.string.register_authentication_error)
    val registerEmailAlreadyExists = stringResource(R.string.register_email_already_exists)
    val registerGenericError = stringResource(R.string.register_generic_error)
    val registerGenericRetryError = stringResource(R.string.register_generic_retry_error)

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

        // MODIFICADO - MULTIDIOMA:
        // Las validaciones conservan exactamente las mismas condiciones.
        // Solo los mensajes visibles provienen de strings_register.xml.
        if (firstName.isBlank()) {
            firstNameError = registerFirstNameRequired
            hasError = true
        } else if (firstName.trim().length < 2) {
            firstNameError = registerMinimumTwoCharacters
            hasError = true
        }

        if (lastName.isBlank()) {
            lastNameError = registerLastNameRequired
            hasError = true
        } else if (lastName.trim().length < 2) {
            lastNameError = registerMinimumTwoCharacters
            hasError = true
        }

        if (email.isBlank()) {
            emailError = registerEmailRequired
            hasError = true
        } else if (!isValidEmail(email)) {
            emailError = registerInvalidEmail
            hasError = true
        }

        if (password.isBlank()) {
            passwordError = registerPasswordRequired
            hasError = true
        } else {
            val validation = validatePassword(password)
            if (!validation.hasMinLength) {
                passwordError = registerPasswordMinLength
                hasError = true
            } else if (!validation.hasUppercase) {
                passwordError = registerPasswordUppercase
                hasError = true
            } else if (!validation.hasNumber) {
                passwordError = registerPasswordNumber
                hasError = true
            }
        }

        if (confirmPassword.isBlank()) {
            confirmPasswordError = registerConfirmPasswordRequired
            hasError = true
        } else if (password != confirmPassword) {
            confirmPasswordError = registerPasswordsDoNotMatch
            hasError = true
        }

        if (phone.isBlank()) {
            phoneError = registerPhoneRequired
            hasError = true
        } else if (phone.length < 9) {
            phoneError = registerInvalidPhone
            hasError = true
        }

        if (selectedDistrict.isEmpty()) {
            districtError = registerDistrictRequired
            hasError = true
        }

        if (selectedRole.isEmpty()) {
            roleError = registerRoleRequired
            hasError = true
        }

        if (selfieUri == null) {
            selfieError = registerPhotoRequired
            hasError = true
        }

        if (selectedRole == "technician") {
            if (dni.isBlank()) {
                dniError = registerDniRequired
                hasError = true
            } else if (dni.length != 8) {
                dniError = registerInvalidDni
                hasError = true
            }
            if (selectedSpecialties.isEmpty()) {
                specialtyError = registerSpecialtyRequired
                hasError = true
            }
        }

        if (!aceptoTerminos) {
            terminosError = registerTermsRequired
            hasError = true
        }

        if (!esMayorDeEdad) {
            edadError = registerAdultRequired
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

                                            // MODIFICADO - MULTIDIOMA:
                                            // La operación de Firestore no cambia; solo el mensaje.
                                            errorMessage = registerSaveDataError
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

                                                // MODIFICADO - MULTIDIOMA:
                                                // La carga a Cloudinary no cambia; solo el mensaje.
                                                errorMessage = registerPhotoUploadError
                                            }
                                        )
                                    }
                                } else {
                                    saveUser("")
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false

                                // MODIFICADO - MULTIDIOMA:
                                // Firebase Authentication conserva el mismo flujo.
                                errorMessage = registerAuthenticationError
                            }

                    } else {
                        isLoading = false
                        // MODIFICADO - MULTIDIOMA:
                        // Los códigos HTTP y la lógica de Retrofit permanecen iguales.
                        when {
                            response.code() == 400 ->
                                emailError = registerEmailAlreadyExists
                            else ->
                                errorMessage = registerGenericRetryError
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = e.message ?: registerGenericError
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

            // MULTIDIOMA:
            // Desde este punto se sustituyen únicamente textos visibles por recursos XML.
            // No se modifican botones, colores, iconos, estados ni navegación.
            Text(
                // MODIFICADO - MULTIDIOMA:
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                // MODIFICADO - MULTIDIOMA:
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selección de rol
            Text(
                // MODIFICADO - MULTIDIOMA:
                text = stringResource(R.string.register_role_question),
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
                    // NUEVO - MULTIDIOMA:
                    // roleCode mantiene la lógica de la imagen sin depender del título traducido.
                    roleCode = "client",
                    // MODIFICADO - MULTIDIOMA:
                    title = stringResource(R.string.register_role_client),
                    description = stringResource(R.string.register_role_client_description),
                    emoji = "",
                    isSelected = selectedRole == "client",
                    color = ClientColor,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = "client"; roleError = "" }
                )
                RoleCard(
                    // NUEVO - MULTIDIOMA:
                    // roleCode mantiene la lógica de la imagen sin depender del título traducido.
                    roleCode = "technician",
                    // MODIFICADO - MULTIDIOMA:
                    title = stringResource(R.string.register_role_technician),
                    description = stringResource(R.string.register_role_technician_description),
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
                    // MODIFICADO - MULTIDIOMA:
                    label = stringResource(R.string.register_first_names),
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
                    // MODIFICADO - MULTIDIOMA:
                    label = stringResource(R.string.register_last_names),
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
                // MODIFICADO - MULTIDIOMA:
                label = stringResource(R.string.register_email),
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
                // MODIFICADO - MULTIDIOMA:
                label = stringResource(R.string.register_password),
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
                // MODIFICADO - MULTIDIOMA:
                label = stringResource(R.string.register_confirm_password),
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
                // MODIFICADO - MULTIDIOMA:
                label = stringResource(R.string.register_phone_number),
                isError = phoneError.isNotEmpty(),
                errorMessage = phoneError,
                keyboardType = KeyboardType.Number
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Distrito
            Text(
                // MODIFICADO - MULTIDIOMA:
                text = stringResource(R.string.register_district),
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
                    // MODIFICADO - MULTIDIOMA:
                    // El distrito elegido sigue siendo el mismo dato; solo se traduce el placeholder.
                    text = if (selectedDistrict.isEmpty()) {
                        stringResource(R.string.register_select_district)
                    } else {
                        selectedDistrict
                    },
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
                // MODIFICADO - MULTIDIOMA:
                text = stringResource(R.string.register_profile_photo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                // MODIFICADO - MULTIDIOMA:
                text = stringResource(R.string.register_profile_photo_description),
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
                        // MODIFICADO - MULTIDIOMA:
                        contentDescription = stringResource(R.string.register_selfie),
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
                                // MODIFICADO - MULTIDIOMA:
                                contentDescription = stringResource(R.string.register_retake_selfie),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.register_take_another_photo),
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
                    Text(
                        // MODIFICADO - MULTIDIOMA:
                        text = stringResource(R.string.register_take_selfie_now)
                    )
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
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.register_technician_data),
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
                    // MODIFICADO - MULTIDIOMA:
                    label = stringResource(R.string.register_dni),
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
                    // MODIFICADO - MULTIDIOMA:
                    label = stringResource(R.string.register_years_experience),
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Especialidades
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(
                        R.string.register_specialties_max,
                        MAX_SPECIALTIES
                    ),
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
                        // MODIFICADO - MULTIDIOMA:
                        // Mantiene el mismo conteo y solo traduce el texto visible.
                        text = if (selectedSpecialties.isEmpty()) {
                            stringResource(R.string.register_select_specialties)
                        } else if (selectedSpecialties.size == 1) {
                            stringResource(
                                R.string.register_one_specialty_selected,
                                selectedSpecialties.size
                            )
                        } else {
                            stringResource(
                                R.string.register_multiple_specialties_selected,
                                selectedSpecialties.size
                            )
                        },
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
                                        // MODIFICADO - MULTIDIOMA:
                                        contentDescription = stringResource(R.string.register_remove),
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
                            // MODIFICADO - MULTIDIOMA:
                            text = stringResource(R.string.register_terms_prefix),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            // MODIFICADO - MULTIDIOMA:
                            text = stringResource(R.string.register_terms_and_conditions),
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
                        // MODIFICADO - MULTIDIOMA:
                        text = stringResource(R.string.register_adult_confirmation),
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
                // MODIFICADO - MULTIDIOMA:
                // El botón mantiene la misma función register().
                text = stringResource(R.string.register_create_account_button),
                onClick = { register() },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.register_already_have_account),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.register_login),
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
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.register_select_district),
                    fontWeight = FontWeight.Bold,
                    color = onSurface // ← antes sin color (heredaba negro)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            // MODIFICADO - MULTIDIOMA:
                            text = stringResource(R.string.register_search_district),
                            color = onSurface.copy(alpha = 0.5f)
                        )
                    },
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
                        // MODIFICADO - MULTIDIOMA:
                        text = stringResource(R.string.register_district_not_found),
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
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.register_cancel),
                    color = onSurface.copy(alpha = 0.5f)
                )
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
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.register_select_your_specialties),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(
                        R.string.register_specialty_selection_status,
                        maxSelection,
                        tempSelected.size
                    ),
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
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.register_confirm),
                    color = if (tempSelected.isNotEmpty()) Primary else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.register_cancel),
                    color = TextSecondary
                )
            }
        }
    )
}

@Composable
fun RoleCard(
    // NUEVO - MULTIDIOMA:
    // Código estable usado únicamente para elegir la imagen correcta.
    // Evita comparar contra un título que cambia según el idioma.
    roleCode: String,
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
                    // MODIFICADO - MULTIDIOMA:
                    // La imagen depende del código estable, no del texto traducido.
                    id = if (roleCode == "client") {
                        R.drawable.registro_cliente
                    } else {
                        R.drawable.registro_tecnico
                    }
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