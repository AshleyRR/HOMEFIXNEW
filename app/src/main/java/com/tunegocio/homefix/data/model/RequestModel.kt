package com.tunegocio.homefix.data.model

data class RequestModel(
    val requestId: String = "",
    val clientId: String = "",
    val technicianId: String = "",
    val serviceType: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val address: String = "",

    val district: String = "",
    val reference: String = "",
    val interestedTechnicians: List<String> = emptyList(),

    val status: String = "pendiente",
    val isUrgent: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,

    // Momento exacto en que el técnico marcó "no puede continuar" (antes de que el cliente lo confirme)
    val technicianCanceledAt: Long = 0L
)

