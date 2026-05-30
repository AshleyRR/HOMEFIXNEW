
package com.tunegocio.homefix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colores que cambian según el tema activo
// Usa estos en lugar de los colores hardcodeados
object AppColors {
    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background

    val surface: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val surfaceVariant: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant

    val cardBackground: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val textPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground

    val textSecondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val textHint: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    val border: Color
        @Composable get() = MaterialTheme.colorScheme.outline
}