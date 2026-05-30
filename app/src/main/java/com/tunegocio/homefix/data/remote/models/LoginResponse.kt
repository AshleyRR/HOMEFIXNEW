package com.tunegocio.homefix.data.remote.models

// Datos que devuelve Firebase al hacer login
data class LoginResponse(
    val idToken: String = "",
    val email: String = "",
    val localId: String = "",
    val refreshToken: String = "",
    val expiresIn: String = "",
    val registered: Boolean = false
)