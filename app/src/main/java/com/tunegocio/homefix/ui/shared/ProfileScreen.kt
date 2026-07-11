package com.tunegocio.homefix.ui.shared

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.tunegocio.homefix.R
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.CloudinaryUploader
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.auth.DistrictPickerDialog
import com.tunegocio.homefix.ui.auth.LIMA_DISTRICTS
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.components.HomefixTextField
import com.tunegocio.homefix.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon

// Pantalla de perfil: muestra y permite editar datos del usuario, foto, distrito y cerrar sesión
@Composable
fun ProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    // Colores dinámicos que responden al modo oscuro
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val iconTint = if (isSystemInDarkTheme()) Color.White else Primary

    // Se resuelven aquí (dentro de la composición) para poder usarlos luego
    // dentro de callbacks de Firebase, donde stringResource no está disponible
    val profileUpdateSuccessMessage =
        stringResource(R.string.profile_update_success)
    val profileSaveErrorMessage =
        stringResource(R.string.profile_save_error)
    val profilePhotoUploadErrorMessage =
        stringResource(R.string.profile_photo_upload_error)

    var user by remember { mutableStateOf<UserModel?>(null) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf("") }
    var showDistrictDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    // Determina el color del mensaje sin depender del idioma del texto
    var successMessageIsError by remember { mutableStateOf(false) }

    var newPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    val photoFile = remember { File(context.cacheDir, "profile_${UUID.randomUUID()}.jpg") }
    val photoUriForCamera = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) newPhotoUri = photoUriForCamera
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(photoUriForCamera)
    }

    LaunchedEffect(uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val u = doc.toObject(UserModel::class.java)
                user = u
                val parts = (u?.name ?: "").split(" ", limit = 2)
                firstName = if (u?.lastName?.isNotBlank() == true) u.name else parts.getOrElse(0) { "" }
                lastName = if (u?.lastName?.isNotBlank() == true) u.lastName else parts.getOrElse(1) { "" }
                phone = u?.phone ?: ""
                bio = u?.bio ?: ""
                selectedDistrict = u?.district ?: ""
                isLoading = false
            }
    }

    // Calcula los años de experiencia sumando los años transcurridos desde el registro
    fun calcYearsExp(u: UserModel): Int {
        if (u.createdAt == 0L) return u.yearsExp
        val yearsPassed = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - u.createdAt) / 365
        return u.yearsExp + yearsPassed.toInt()
    }

    // Guarda los cambios del perfil en Firestore, subiendo antes la foto nueva si existe
    fun saveProfile() {
        isSaving = true
        // Actualiza el documento del usuario y muestra el mensaje de éxito o error correspondiente
        fun doSave(photoUrl: String?) {
            val updates = mutableMapOf<String, Any>(
                "name" to firstName.trim(),
                "lastName" to lastName.trim(),
                "phone" to phone.trim(),
                "whatsapp" to phone.trim(),
                "district" to selectedDistrict
            )
            if (photoUrl != null) updates["selfieUrl"] = photoUrl
            if (user?.role == "technician") updates["bio"] = bio.trim()
            db.collection("users").document(uid).update(updates)
                .addOnSuccessListener {
                    isSaving = false
                    successMessage = profileUpdateSuccessMessage
                    successMessageIsError = false
                }
                .addOnFailureListener {
                    isSaving = false
                    successMessage = profileSaveErrorMessage
                    successMessageIsError = true
                }
        }
        if (newPhotoUri != null) {
            isUploadingPhoto = true
            scope.launch {
                val result = CloudinaryUploader.uploadImage(context, newPhotoUri!!, "homefix/selfies")
                isUploadingPhoto = false
                result.fold(
                    onSuccess = { url -> doSave(url) },
                    onFailure = {
                        isSaving = false
                        successMessage = profilePhotoUploadErrorMessage
                        successMessageIsError = true
                    }
                )
            }
        } else doSave(null)
    }

    // Cierra sesión y navega a la pantalla de login limpiando el back stack
    fun logout() {
        auth.signOut()
        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
    }

    if (showDistrictDialog) {
        DistrictPickerDialog(
            districts = LIMA_DISTRICTS,
            selectedDistrict = selectedDistrict,
            onDismiss = { showDistrictDialog = false },
            onConfirm = { district -> selectedDistrict = district; showDistrictDialog = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text(
                    text = stringResource(R.string.profile_logout),
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.profile_logout_question),
                    color = textColor
                )
            },
            confirmButton = {
                TextButton(onClick = { logout() }, colors = ButtonDefaults.textButtonColors(contentColor = Error)) {
                    Text(
                        text = stringResource(R.string.profile_logout),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(
                        text = stringResource(R.string.profile_cancel),
                        color = secondaryText
                    )
                }
            }
        )
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val u = user ?: return

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.profile_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.profile_settings),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Foto de perfil (o inicial del nombre si no hay foto)
            Box(contentAlignment = Alignment.BottomEnd) {
                val photoToShow = newPhotoUri ?: u.selfieUrl.takeIf { it.isNotBlank() }
                if (photoToShow != null) {
                    AsyncImage(
                        model = photoToShow,
                        contentDescription = stringResource(R.string.profile_photo),
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(50.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = RoundedCornerShape(50.dp),
                        color = if (u.role == "technician") TechnicianColor.copy(alpha = 0.15f) else ClientColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = u.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                color = if (u.role == "technician") TechnicianColor else ClientColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.size(32.dp).clickable { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(16.dp),
                    color = Primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.profile_change_photo),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badge rol (el valor guardado en Firebase no cambia, solo su etiqueta visible)
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (u.role == "technician") TechnicianColor.copy(alpha = 0.1f) else ClientColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = if (u.role == "technician") {
                        stringResource(R.string.profile_role_technician)
                    } else {
                        stringResource(R.string.profile_role_client)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (u.role == "technician") TechnicianColor else ClientColor,
                    fontWeight = FontWeight.Medium
                )
            }

            if (u.rating > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_rating_format, u.rating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryText
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Datos personales
            Text(
                text = stringResource(R.string.profile_personal_data),
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomefixTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = stringResource(R.string.profile_first_names),
                    modifier = Modifier.weight(1f)
                )
                HomefixTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = stringResource(R.string.profile_last_names),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = u.email,
                onValueChange = {},
                label = {
                    Text(text = stringResource(R.string.profile_email))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = outlineColor,
                    disabledLabelColor = secondaryText,
                    disabledTextColor = secondaryText
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            HomefixTextField(
                value = phone,
                onValueChange = { phone = it },
                label = stringResource(R.string.profile_whatsapp_number)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Distrito (campo de solo lectura; el diálogo real de selección se abre con el click)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedDistrict,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(text = stringResource(R.string.profile_district))
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.profile_select_district),
                            color = secondaryText
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedLabelColor = secondaryText,
                        unfocusedLabelColor = secondaryText,
                        cursorColor = Color.Transparent,
                        focusedLeadingIconColor = textColor,
                        unfocusedLeadingIconColor = textColor,
                        focusedTrailingIconColor = textColor,
                        unfocusedTrailingIconColor = textColor
                    )
                )
                // Capa invisible para capturar el click y abrir el selector de distrito
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDistrictDialog = true }
                )
            }

            // Sección técnico
            if (u.role == "technician") {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = outlineColor)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.profile_professional_information),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = u.email,
                    onValueChange = {},
                    label = {
                        Text(text = stringResource(R.string.profile_email))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = secondaryText,
                        cursorColor = Color.Transparent
                    )
                )

                if (u.specialties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.profile_specialties),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        u.specialties.take(3).forEach { specialty ->
                            Surface(shape = RoundedCornerShape(8.dp), color = TechnicianColor.copy(alpha = 0.1f)) {
                                Text(text = specialty, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = TechnicianColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Warning, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.profile_years_experience,
                                calcYearsExp(u)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
            }

            if (successMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (successMessageIsError) {
                        Error.copy(alpha = 0.1f)
                    } else {
                        Success.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (successMessageIsError) Error else Success
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HomefixButton(
                text = stringResource(R.string.profile_save_changes),
                onClick = { saveProfile() },
                isLoading = isSaving || isUploadingPhoto,
                color = MaterialTheme.colorScheme.primary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Error)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.profile_logout),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}