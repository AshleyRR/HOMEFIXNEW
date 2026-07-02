package com.tunegocio.homefix.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Configuración de Retrofit
object RetrofitClient {
    private const val BASE_URL = "https://identitytoolkit.googleapis.com/v1/"
    const val API_KEY = "AIzaSyCwaMDT52c9jF5pjb2mzKioX4I_E-Yix9M"
    const val LOGIN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"
    const val REGISTER_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}