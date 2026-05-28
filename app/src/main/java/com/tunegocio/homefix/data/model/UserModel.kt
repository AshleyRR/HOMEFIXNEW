package com.tunegocio.homefix.data.model

data class UserModel(
    val uid: String = "",
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val role: String = "",        // "cliente" o "tecnico"
    val phone: String = "",
    val photoUrl: String = "",
    val selfieUrl: String = "",
    val district: String = "",
    val rating: Float = 0f,
    val createdAt: Long = 0L,

    // Solo técnicos
    val dni: String = "",

    val specialties: List<String> = emptyList(),
    val yearsExp: Int = 0,
    val whatsapp: String = "",
    val isActive: Boolean = false,
    val bio: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)