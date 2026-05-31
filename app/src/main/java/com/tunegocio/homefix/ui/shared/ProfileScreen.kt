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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
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
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.isSystemInDarkTheme

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

    fun calcYearsExp(u: UserModel): Int {
        if (u.createdAt == 0L) return u.yearsExp
        val yearsPassed = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - u.createdAt) / 365
        return u.yearsExp + yearsPassed.toInt()
    }

    fun saveProfile() {
        isSaving = true
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
                .addOnSuccessListener { isSaving = false; successMessage = "Perfil actualizado correctamente" }
                .addOnFailureListener { isSaving = false; successMessage = "Error al guardar" }
        }
        if (newPhotoUri != null) {
            isUploadingPhoto = true
            scope.launch {
                val result = CloudinaryUploader.uploadImage(context, newPhotoUri!!, "homefix/selfies")
                isUploadingPhoto = false
                result.fold(
                    onSuccess = { url -> doSave(url) },
                    onFailure = { isSaving = false; successMessage = "Error al subir la foto" }
                )
            }
        } else doSave(null)
    }

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
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold, color = textColor) },
            text = { Text("¿Estás seguro que deseas cerrar sesión?", color = textColor) },
            confirmButton = {
                TextButton(onClick = { logout() }, colors = ButtonDefaults.textButtonColors(contentColor = Error)) {
                    Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = secondaryText)
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text("Mi perfil", style = MaterialTheme.typography.headlineMedium, color = textColor, fontWeight = FontWeight.Bold)
                IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Configuraciones", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Foto de perfil
            Box(contentAlignment = Alignment.BottomEnd) {
                val photoToShow = newPhotoUri ?: u.selfieUrl.takeIf { it.isNotBlank() }
                if (photoToShow != null) {
                    AsyncImage(
                        model = photoToShow,
                        contentDescription = "Foto de perfil",
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
                            Icon(Icons.Default.CameraAlt, contentDescription = "Cambiar foto", tint = Color.White, modifier = Modifier.size(16.dp))

                        }
                    }
                }

            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badge rol
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (u.role == "technician") TechnicianColor.copy(alpha = 0.1f) else ClientColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = if (u.role == "technician") "Técnico" else "Cliente",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (u.role == "technician") TechnicianColor else ClientColor,
                    fontWeight = FontWeight.Medium
                )
            }

            if (u.rating > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("⭐ ${"%.1f".format(u.rating)} calificación", style = MaterialTheme.typography.bodyMedium, color = secondaryText)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Datos personales
            Text("Datos personales", style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomefixTextField(value = firstName, onValueChange = { firstName = it }, label = "Nombres", modifier = Modifier.weight(1f))
                HomefixTextField(value = lastName, onValueChange = { lastName = it }, label = "Apellidos", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = u.email,
                onValueChange = {},
                label = { Text("Correo electrónico") },
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

            HomefixTextField(value = phone, onValueChange = { phone = it }, label = "Número de WhatsApp")
            Spacer(modifier = Modifier.height(12.dp))

// Distrito
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedDistrict,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Distrito") },
                    placeholder = { Text("Selecciona tu distrito", color = secondaryText) },
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
                // Capa invisible para capturar el click
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

                Text("Información profesional", style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = u.email,
                    onValueChange = {},
                    label = { Text("Correo electrónico") },
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
                    Text("Especialidades", style = MaterialTheme.typography.bodyMedium, color = textColor, fontWeight = FontWeight.Medium)
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
                        Text("${calcYearsExp(u)} años de experiencia", style = MaterialTheme.typography.bodyMedium, color = textColor)
                    }
                }
            }

            if (successMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (successMessage.contains("Error")) Error.copy(alpha = 0.1f) else Success.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (successMessage.contains("Error")) Error else Success
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HomefixButton(
                text = "Guardar cambios",
                onClick = { saveProfile() },
                isLoading = isSaving || isUploadingPhoto,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}