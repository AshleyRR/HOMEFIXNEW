package com.tunegocio.homefix.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

// Composable que solicita permisos en runtime y notifica si fueron concedidos o denegados
@Composable
fun RequestPermissions(
    permissions: List<String>,
    onGranted: () -> Unit,
    onDenied: () -> Unit = {}
) {
    val context = LocalContext.current

    // Lanzador del diálogo de permisos, evalúa el resultado y dispara el callback correspondiente
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) onGranted() else onDenied()
    }

    // Verifica permisos faltantes al iniciar y los solicita si es necesario
    LaunchedEffect(Unit) {
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) onGranted()
        else launcher.launch(notGranted.toTypedArray())
    }
}