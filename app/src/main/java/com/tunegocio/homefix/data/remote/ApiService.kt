package com.tunegocio.homefix.data.remote

import com.tunegocio.homefix.data.remote.models.LoginRequest
import com.tunegocio.homefix.data.remote.models.LoginResponse
import com.tunegocio.homefix.data.remote.models.RegisterRequest
import com.tunegocio.homefix.data.remote.models.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

// Interfaz con los endpoints de Firebase Auth REST API
interface ApiService {

    // Login con email y contraseña
    @POST
    suspend fun login(
        @Url url: String,
        @Query("key") apiKey: String,
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // Registro con email y contraseña
    @POST
    suspend fun register(
        @Url url: String,
        @Query("key") apiKey: String,
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

}