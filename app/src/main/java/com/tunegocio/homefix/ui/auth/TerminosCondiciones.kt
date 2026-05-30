package com.tunegocio.homefix.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunegocio.homefix.ui.theme.*


const val TEXTO_TERMINOS_CONDICIONES = """
TÉRMINOS Y CONDICIONES DE USO — HOMEFIX

Última actualización: 2026

1. ACEPTACIÓN DE TÉRMINOS
Al registrarte en HomeFix aceptas estos términos. Si no estás de acuerdo, no uses la aplicación.

2. DESCRIPCIÓN DEL SERVICIO
HomeFix es una plataforma digital que conecta clientes con técnicos independientes de servicios del hogar como electricidad, gasfitería, carpintería, pintura y otros. HomeFix no es empleador de los técnicos ni garantiza la calidad del servicio prestado.

3. REGISTRO DE USUARIOS
Debes proporcionar información veraz, completa y actualizada al registrarte. Eres responsable de mantener la confidencialidad de tu cuenta y contraseña. Notifica inmediatamente cualquier uso no autorizado de tu cuenta.

4. ROLES DE USUARIO
- Cliente: persona que publica solicitudes de servicio del hogar.
- Técnico: persona que ofrece y presta servicios del hogar de forma independiente.

5. TÉCNICOS INDEPENDIENTES
Los técnicos registrados en HomeFix son trabajadores independientes. HomeFix no garantiza sus servicios, certificaciones ni es responsable por daños, accidentes o perjuicios causados durante la prestación del servicio.

6. PAGOS Y TARIFAS
Los pagos y tarifas se acuerdan directamente entre cliente y técnico. HomeFix no interviene en transacciones económicas ni cobra comisión actualmente.

7. PRIVACIDAD Y DATOS
Tu información personal se usa únicamente para el funcionamiento de la plataforma. No vendemos ni compartimos datos con terceros sin tu consentimiento. Las fotos de perfil y selfies se almacenan de forma segura.

8. CONDUCTA PROHIBIDA
Está prohibido usar HomeFix para actividades ilegales, fraudulentas, discriminatorias o que perjudiquen a otros usuarios. El incumplimiento puede resultar en la suspensión permanente de la cuenta.

9. CALIFICACIONES Y RESEÑAS
Las calificaciones deben ser honestas y basadas en experiencias reales. Está prohibido manipular el sistema de calificaciones.

10. MODIFICACIONES
HomeFix puede modificar estos términos en cualquier momento. Se notificará por email ante cambios importantes.

11. MAYORÍA DE EDAD
El uso de HomeFix está restringido a personas mayores de 18 años.

12. CONTACTO Y SOPORTE
Para consultas o reportes: homefix.soporte@gmail.com
"""



@Composable
fun DialogoTerminos(
    onAceptar: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(
                text = "Términos y Condiciones",
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
                    text = TEXTO_TERMINOS_CONDICIONES,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onAceptar
            ) {
                Text(
                    text = "Acepto los términos",
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(
                    text = "Cerrar",
                    color = TextSecondary
                )
            }
        }
    )
}