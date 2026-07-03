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
Términos y Condiciones de Uso — HOMEFIX
Última actualización: 2026 | Aplicable en Lima Metropolitana, Perú

1. Aceptación
Al registrarte en HOMEFIX declaras ser mayor de 18 años y aceptar estos Términos y Condiciones, así como la Política de Privacidad. El uso continuo de la aplicación implica la aceptación de cualquier actualización futura.
2. Descripción del servicio
HOMEFIX es una plataforma digital de intermediación que conecta clientes con técnicos independientes de servicios del hogar en Lima Metropolitana. La aplicación no presta servicios técnicos directamente, no emplea a los técnicos registrados, no supervisa la ejecución del trabajo ni procesa pagos. Los acuerdos económicos y condiciones del servicio son responsabilidad exclusiva de las partes involucradas.
3. Registro y cuenta
El usuario debe proporcionar información verdadera, completa y actualizada al registrarse. Está prohibido crear cuentas falsas o duplicadas, suplantar identidades o utilizar la plataforma para fines ajenos a su objeto. El usuario es responsable de la confidencialidad de sus credenciales de acceso.
4. Datos personales y privacidad
HOMEFIX recopila únicamente los datos necesarios para el funcionamiento de la plataforma, incluyendo información de identificación, contacto, ubicación GPS, fotografías e historial de solicitudes. Estos datos se tratan conforme a la Ley N.° 29733 de Protección de Datos Personales del Perú. HOMEFIX no vende ni comparte datos personales con terceros salvo los servicios técnicos necesarios para su operación: Firebase Authentication y Firestore para autenticación y almacenamiento, Cloudinary para imágenes, Google Maps para geolocalización y WhatsApp como canal externo de comunicación entre usuarios.
5. Permisos del dispositivo
La aplicación solicitará permisos de cámara, galería, ubicación y notificaciones únicamente cuando sean necesarios para el funcionamiento de sus funcionalidades. Denegar alguno de estos permisos puede limitar ciertas funciones de la app.
6. Obligaciones de los usuarios
Los clientes se comprometen a publicar solicitudes reales, describir claramente el servicio y calificar de forma objetiva. Los técnicos se comprometen a declarar información veraz sobre su identidad y especialidades, mantener actualizada su disponibilidad y actuar con profesionalismo. Cualquier daño, incumplimiento o desacuerdo derivado de la prestación del servicio es responsabilidad exclusiva de las partes.
7. Seguridad
HOMEFIX implementa medidas técnicas razonables para proteger la información de los usuarios, incluyendo autenticación, control de acceso por rol y reglas de seguridad en base de datos. Sin embargo, ningún sistema conectado a internet garantiza seguridad absoluta. HOMEFIX no se responsabiliza por pérdida de credenciales, mal uso de la cuenta o ataques de terceros.
8. Calificaciones y sanciones
Las calificaciones deben basarse en experiencias reales y expresarse con respeto. HOMEFIX podrá retirar comentarios ofensivos, falsos o difamatorios, así como suspender o eliminar cuentas que incumplan estos términos, registren información falsa o hagan uso indebido de la plataforma.
9. Notificaciones
HOMEFIX enviará notificaciones relacionadas con el estado de las solicitudes, selección de técnicos, verificación de cuenta y otros eventos relevantes. El usuario puede gestionar sus preferencias de notificación desde la configuración de la aplicación. Desactivarlas puede afectar el seguimiento oportuno de los servicios.
10. Derechos del usuario
El usuario puede ejercer sus derechos de acceso, rectificación, cancelación y oposición sobre sus datos personales contactando al equipo de soporte de HOMEFIX. En caso de solicitar la eliminación de cuenta, los datos serán suprimidos salvo aquellos que deban conservarse por razones legales o de seguridad.
11. Modificaciones y legislación
HOMEFIX puede modificar estos términos por razones técnicas, legales u operativas, comunicándolo a través de la aplicación. Estos términos se rigen por las leyes de la República del Perú y su ámbito funcional se limita a Lima Metropolitana.
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