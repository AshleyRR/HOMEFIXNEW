package com.tunegocio.homefix.data.remote.models

// Datos que devuelve Firebase al registrarse
data class RegisterResponse(
    val idToken: String = "",
    val email: String = "",
    val localId: String = "",
    val refreshToken: String = ""
)