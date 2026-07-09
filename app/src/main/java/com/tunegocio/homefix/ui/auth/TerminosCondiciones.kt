package com.tunegocio.homefix.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.tunegocio.homefix.ui.theme.*

// NUEVO - MULTIDIOMA:
// Permite obtener el título, el contenido legal y los botones desde
// strings_terms.xml según el idioma activo de la aplicación.
import androidx.compose.ui.res.stringResource

// NUEVO - MULTIDIOMA:
// Permite acceder a las claves declaradas en strings_terms.xml.
import com.tunegocio.homefix.R


// MODIFICADO - MULTIDIOMA:
// El texto legal ya no se mantiene como una constante escrita directamente
// en Kotlin. Ahora se encuentra en los archivos strings_terms.xml de
// español, inglés y portugués.
//
// No se modifica la aceptación de términos, el cierre del diálogo,
// los botones ni la lógica utilizada por RegisterScreen.kt.
@Composable
fun DialogoTerminos(
    onAceptar: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(
                // MODIFICADO - MULTIDIOMA:
                // Título obtenido desde el XML del idioma activo.
                text = stringResource(R.string.terms_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    // El contenido legal completo se obtiene desde strings_terms.xml.
                    // El desplazamiento vertical y el diseño permanecen iguales.
                    text = stringResource(R.string.terms_full_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        confirmButton = {
            TextButton(
                // La función onAceptar se conserva exactamente igual.
                onClick = onAceptar
            ) {
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.terms_accept),
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                // La función onCerrar se conserva exactamente igual.
                onClick = onCerrar
            ) {
                Text(
                    // MODIFICADO - MULTIDIOMA:
                    text = stringResource(R.string.terms_close),
                    color = TextSecondary
                )
            }
        }
    )
}
