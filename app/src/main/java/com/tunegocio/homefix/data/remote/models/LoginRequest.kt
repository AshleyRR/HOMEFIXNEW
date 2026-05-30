package com.tunegocio.homefix.data.remote.models

// Datos que se envian al hacer login
data class LoginRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true
)