package com.tunegocio.homefix.data.remote.models

// Datos que se envian al registrarse
data class RegisterRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true
)