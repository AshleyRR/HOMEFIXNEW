package com.tunegocio.homefix.navigation

object Routes {
    // Auth
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VERIFICAR_EMAIL = "verificar_email"
    const val OLVIDE_CONTRASENA = "olvide_contrasena"

    // Cliente
    const val HOME_CLIENT = "home_client"
    const val NEW_REQUEST = "new_request"
    const val TECHNICIAN_LIST = "technician_list"
    const val REQUEST_TRACKING = "request_tracking/{requestId}"

    const val SELECCIONAR_UBICACION = "seleccionar_ubicacion"

    // Técnico
    const val HOME_TECHNICIAN = "home_technician"
    const val REQUEST_DETAIL = "request_detail/{requestId}"
    const val EARNINGS = "earnings"

    const val MY_APPLICATIONS = "my_applications"

    // Compartidas (ambos roles)
    const val PROFILE = "profile"
    const val HISTORY = "history"
    const val RATING = "rating/{requestId}"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"

    // Helpers para rutas con parámetros
    fun requestTracking(requestId: String) = "request_tracking/$requestId"
    fun requestDetail(requestId: String) = "request_detail/$requestId"
    fun rating(requestId: String) = "rating/$requestId"
}