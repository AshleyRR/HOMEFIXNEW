package com.tunegocio.homefix.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.tunegocio.homefix.ui.auth.SplashScreen
import com.tunegocio.homefix.ui.auth.LoginScreen
import com.tunegocio.homefix.ui.auth.RegisterScreen
import com.tunegocio.homefix.ui.auth.VerificarEmailScreen
import com.tunegocio.homefix.ui.auth.OlvideContrasenaScreen

import com.tunegocio.homefix.ui.client.HomeClientScreen
import com.tunegocio.homefix.ui.client.NewRequestScreen
import com.tunegocio.homefix.ui.client.TechnicianListScreen
import com.tunegocio.homefix.ui.client.RequestTrackingScreen

import com.tunegocio.homefix.ui.technician.HomeTechnicianScreen
import com.tunegocio.homefix.ui.technician.RequestDetailScreen
import com.tunegocio.homefix.ui.technician.EarningsScreen

import com.tunegocio.homefix.ui.shared.ProfileScreen
import com.tunegocio.homefix.ui.shared.SettingsScreen
import com.tunegocio.homefix.ui.shared.NotificationsScreen
import com.tunegocio.homefix.ui.shared.RatingScreen
import com.tunegocio.homefix.ui.shared.HistoryScreen

import com.tunegocio.homefix.viewmodel.UbicacionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunegocio.homefix.ui.client.SeleccionarUbicacionScreen


@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {

    val ubicacionViewModel: UbicacionViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH


    ) {

        //  Autenticacion
        composable(Routes.SPLASH) {
            SplashScreen(navController = navController)
        }
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController)
        }
        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController)
        }
        composable(Routes.VERIFICAR_EMAIL) {
            VerificarEmailScreen(navController = navController)
        }
        composable(Routes.OLVIDE_CONTRASENA) {
            OlvideContrasenaScreen(navController = navController)
        }

        //  Cliente
        composable(Routes.HOME_CLIENT) {
            HomeClientScreen(navController = navController)
        }
        composable(Routes.NEW_REQUEST) {
            NewRequestScreen(
                navController = navController,
                ubicacionViewModel = ubicacionViewModel
            )
        }
        composable(Routes.SELECCIONAR_UBICACION) {
            SeleccionarUbicacionScreen(
                navController = navController,
                ubicacionViewModel = ubicacionViewModel
            )
        }
        composable(Routes.TECHNICIAN_LIST) {
            TechnicianListScreen(navController = navController)
        }
        composable(Routes.REQUEST_TRACKING) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RequestTrackingScreen(
                navController = navController,
                requestId = requestId
            )
        }


        // ── Técnico
        composable(Routes.HOME_TECHNICIAN) {
            HomeTechnicianScreen(navController = navController)
        }
        composable(Routes.REQUEST_DETAIL) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RequestDetailScreen(
                navController = navController,
                requestId = requestId
            )
        }
        composable(Routes.EARNINGS) {
            EarningsScreen(navController = navController)
        }

        // Compartidas (ambos roles)
        composable(Routes.PROFILE) {
            ProfileScreen(navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(navController = navController)
        }
        composable(Routes.HISTORY) {
            HistoryScreen(navController = navController)
        }
        composable(Routes.RATING) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RatingScreen(navController = navController, requestId = requestId)
        }
    }
}