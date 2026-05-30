package com.tunegocio.homefix.data.remote.models

// Modelo del error que devuelve Firebase
data class FirebaseErrorResponse(
    val error: FirebaseError = FirebaseError()
)

data class FirebaseError(
    val code: Int = 0,
    val message: String = "",
    val status: String = ""
)