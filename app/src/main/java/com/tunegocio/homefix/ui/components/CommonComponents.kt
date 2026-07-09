package com.tunegocio.homefix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tunegocio.homefix.ui.theme.*

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.KeyboardType

import com.tunegocio.homefix.ui.theme.Success
import com.tunegocio.homefix.ui.theme.Error
import com.tunegocio.homefix.ui.theme.TextSecondary
import com.tunegocio.homefix.ui.theme.Primary
import com.tunegocio.homefix.ui.theme.CardBorder

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// NUEVO - MULTIDIOMA:
// Permite obtener los textos de accesibilidad y requisitos de contraseña
// desde strings_components.xml según el idioma activo.
import androidx.compose.ui.res.stringResource

// NUEVO - MULTIDIOMA:
// Permite acceder a las claves definidas en strings_components.xml.
import com.tunegocio.homefix.R


// Botón principal
@Composable
fun HomefixButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    color: Color = Primary,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

// Botón secundario
@Composable
fun HomefixOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

// Campo de texto
@Composable
fun HomefixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (isPassword && !isPasswordVisible)
                PasswordVisualTransformation()
            else
                VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            // Ícono del ojo — solo aparece en campos de contraseña
            trailingIcon = {
                if (isPassword) {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible)
                                Icons.Default.VisibilityOff
                            else
                                Icons.Default.Visibility,

                            // MODIFICADO - MULTIDIOMA:
                            // Solo se traduce la descripción accesible del icono.
                            // La lógica para mostrar u ocultar la contraseña no cambia.
                            contentDescription = if (isPasswordVisible) {
                                stringResource(R.string.component_hide_password)
                            } else {
                                stringResource(R.string.component_show_password)
                            },

                            tint = TextSecondary
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = CardBorder,
                focusedLabelColor = Primary,
                errorBorderColor = Error,
                errorLabelColor = Error
            )
        )
        if (isError && errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

// Tarjeta base
@Composable
fun HomefixCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

fun isValidEmail(email: String): Boolean {
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return emailRegex.matches(email.trim())
}

// Nueva función
data class PasswordValidation(
    val hasMinLength: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasNumber: Boolean = false
) {
    val isValid get() = hasMinLength && hasUppercase && hasNumber
}

fun validatePassword(password: String): PasswordValidation {
    return PasswordValidation(
        hasMinLength = password.length >= 6,
        hasUppercase = password.any { it.isUpperCase() },
        hasNumber = password.any { it.isDigit() }
    )
}

@Composable
fun PasswordRequirements(password: String) {
    val validation = validatePassword(password)

    if (password.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        PasswordRequirementItem(
            // MODIFICADO - MULTIDIOMA:
            // La validación sigue usando validation.hasMinLength.
            // Solo cambia el texto visible.
            text = stringResource(R.string.component_password_min_length),
            isMet = validation.hasMinLength
        )
        PasswordRequirementItem(
            // MODIFICADO - MULTIDIOMA:
            // La validación de mayúscula no cambia.
            text = stringResource(R.string.component_password_uppercase),
            isMet = validation.hasUppercase
        )
        PasswordRequirementItem(
            // MODIFICADO - MULTIDIOMA:
            // La validación numérica no cambia.
            text = stringResource(R.string.component_password_number),
            isMet = validation.hasNumber
        )
    }
}

@Composable
fun PasswordRequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Círculo verde o rojo
        Surface(
            modifier = Modifier.size(8.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = if (isMet) Success else Error.copy(alpha = 0.5f)
        ) {}
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isMet) Success else TextSecondary
        )
    }
}
