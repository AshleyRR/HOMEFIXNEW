package com.tunegocio.homefix.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ViewModel que preserva el estado del formulario de nueva solicitud
// Sobrevive la navegación al mapa y de vuelta
class SolicitudViewModel : ViewModel() {

    // Tipo de servicio seleccionado
    private val _serviceType = MutableStateFlow("")
    val serviceType: StateFlow<String> = _serviceType

    // Descripción del problema
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    // Referencia de ubicación
    private val _reference = MutableStateFlow("")
    val reference: StateFlow<String> = _reference

    // Es urgente
    private val _isUrgent = MutableStateFlow(false)
    val isUrgent: StateFlow<Boolean> = _isUrgent

    // Fotos seleccionadas — se guardan como strings de URI
    private val _photoUris = MutableStateFlow<List<String>>(emptyList())
    val photoUris: StateFlow<List<String>> = _photoUris

    // Funciones para actualizar cada campo
    fun setServiceType(value: String) { _serviceType.value = value }
    fun setDescription(value: String) { _description.value = value }
    fun setReference(value: String) { _reference.value = value }
    fun setIsUrgent(value: Boolean) { _isUrgent.value = value }

    fun addPhoto(uri: Uri) {
        if (_photoUris.value.size < 2) {
            _photoUris.value = _photoUris.value + uri.toString()
        }
    }

    fun removePhoto(index: Int) {
        _photoUris.value = _photoUris.value.filterIndexed { i, _ -> i != index }
    }

    // Limpiar el formulario después de publicar la solicitud
    fun limpiar() {
        _serviceType.value = ""
        _description.value = ""
        _reference.value = ""
        _isUrgent.value = false
        _photoUris.value = emptyList()
    }
}