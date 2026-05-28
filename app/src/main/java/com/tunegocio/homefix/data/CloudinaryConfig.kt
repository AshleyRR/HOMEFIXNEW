package com.tunegocio.homefix.data

object CloudinaryConfig {
    const val CLOUD_NAME = "dnaul8kll"
    const val UPLOAD_PRESET = "homefix_upload"

    fun getUploadUrl(): String {
        return "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
    }
}