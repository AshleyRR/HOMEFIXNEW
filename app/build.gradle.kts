plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    //alias(libs.plugins.ksp)

}

android {
    namespace = "com.tunegocio.homefix"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tunegocio.homefix"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)

    // Navegación
    implementation(libs.navigation.compose)
    implementation(libs.viewmodel.compose)

    // GPS y Maps
    implementation(libs.maps.services)
    implementation(libs.location.services)

    // Cámara
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Imágenes
    implementation(libs.coil.compose)

    // Corrutinas
    implementation(libs.coroutines.android)
    implementation(libs.androidx.compose.foundation)

    // Extras
    implementation(libs.compose.icons.extended)
    implementation(libs.okhttp)

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // OSMDroid
    implementation(libs.osmdroid)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)


    //implementation(libs.room.runtime)
    //implementation(libs.room.ktx)
    //annotationProcessor(libs.room.compiler)
    //ksp(libs.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)



}