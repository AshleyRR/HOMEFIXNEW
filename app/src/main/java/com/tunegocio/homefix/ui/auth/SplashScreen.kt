package com.tunegocio.homefix.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.R
import com.tunegocio.homefix.navigation.Routes
import kotlinx.coroutines.delay

// Pantalla de bienvenida (Splash): anima el logo, avanza una barra de progreso
// y luego revisa la sesión/rol del usuario para redirigir a Login, Home Cliente o Home Técnico.
@Composable
fun SplashScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Dispara las animaciones de fade y escala
    var startAnimation by remember { mutableStateOf(false) }
    // Avance de la barra de progreso (0f a 1f)
    var progress by remember { mutableFloatStateOf(0f) }

    // Fade in del logo
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = ""
    )

    // Zoom sutil del logo
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.92f,
        animationSpec = tween(1000),
        label = ""
    )

    // Se ejecuta una sola vez al entrar: corre la animación de progreso y luego
    // decide a qué pantalla navegar según el estado de sesión y el rol del usuario
    LaunchedEffect(Unit) {

        startAnimation = true

        // Incrementa el progreso de 1% a 100% con un pequeño delay entre pasos
        for (i in 1..100) {
            progress = i / 100f
            delay(15)
        }

        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Usuario logueado: consulta su documento en Firestore para saber el rol
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role") ?: "client"

                    // Redirige a Home Técnico o Home Cliente según el rol, limpiando el back stack del splash
                    if (role == "technician") {
                        navController.navigate(Routes.HOME_TECHNICIAN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.HOME_CLIENT) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                }
                .addOnFailureListener {
                    // Si falla la consulta a Firestore, cae al Login como fallback
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }

        } else {
            // Sin usuario autenticado, va directo al Login
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        }
    }

    // Contenedor principal, ocupa todo el tamaño disponible con fondo blanco
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // Imagen de splash con fade y zoom aplicados
        Image(
            painter = painterResource(R.drawable.homefix_splash),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .scale(scale),
            contentScale = ContentScale.Crop
        )

        // Texto "Cargando..." + barra de progreso, anclados abajo
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Cargando...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(280.dp)
                    .height(14.dp),
                color = Color(0xFFD4AF37),
                trackColor = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}