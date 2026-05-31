package com.tunegocio.homefix.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ViewModel compartido para pasar ubicación entre pantallas
class UbicacionViewModel : ViewModel() {

    private val _lat = MutableStateFlow(0.0)
    val lat: StateFlow<Double> = _lat

    private val _lng = MutableStateFlow(0.0)
    val lng: StateFlow<Double> = _lng

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address

    private val _confirmada = MutableStateFlow(false)
    val confirmada: StateFlow<Boolean> = _confirmada

    fun actualizarUbicacion(lat: Double, lng: Double, address: String) {
        _lat.value = lat
        _lng.value = lng
        _address.value = address
    }

    fun confirmar() {
        _confirmada.value = true
    }

    fun resetConfirmacion() {
        _confirmada.value = false
    }
}