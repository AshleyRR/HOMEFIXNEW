package com.tunegocio.homefix.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunegocio.homefix.R
import com.tunegocio.homefix.ui.theme.*

// Diálogo de Términos y Condiciones
@Composable
fun DialogoTerminos(
    onAceptar: () -> Unit,
    onCerrar: () -> Unit
) {
    // Controla la posición del scroll dentro del texto legal
    val scrollState = rememberScrollState()

    // true cuando el usuario llega al final del texto; habilita el botón de aceptar
    val llegoAlFinal by remember {
        derivedStateOf {
            scrollState.maxValue > 0 &&
                    scrollState.value >= scrollState.maxValue
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(
                text = stringResource(R.string.terms_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            // Contenido legal con scroll vertical
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(R.string.terms_full_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        confirmButton = {
            // Habilitado solo cuando se terminó de leer el texto
            TextButton(
                enabled = llegoAlFinal,
                onClick = onAceptar
            ) {
                Text(
                    text = stringResource(R.string.terms_accept),
                    color = if (llegoAlFinal) Primary else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCerrar
            ) {
                Text(
                    text = stringResource(R.string.terms_close),
                    color = TextSecondary
                )
            }
        }
    )
}