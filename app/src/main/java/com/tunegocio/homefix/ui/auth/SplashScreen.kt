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

@Composable
fun SplashScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var startAnimation by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = ""
    )

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.92f,
        animationSpec = tween(1000),
        label = ""
    )

    LaunchedEffect(Unit) {

        startAnimation = true

        // Animación de la barra
        for (i in 1..100) {
            progress = i / 100f
            delay(15)
        }

        val currentUser = auth.currentUser

        if (currentUser != null) {

            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { doc ->

                    val role = doc.getString("role") ?: "client"

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

                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }

        } else {

            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }

        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Image(
            painter = painterResource(R.drawable.homefix_splash),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .scale(scale),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp), // Antes era 40.dp
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
                    .height(14.dp), // Antes era 6.dp
                color = Color(0xFFD4AF37),
                trackColor = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}