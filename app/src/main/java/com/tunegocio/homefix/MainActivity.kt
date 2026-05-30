package com.tunegocio.homefix

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tunegocio.homefix.data.UserPreferences
import com.tunegocio.homefix.navigation.AppNavigation
import com.tunegocio.homefix.ui.theme.HomefixTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userPreferences = UserPreferences(applicationContext)

        setContent {
            val darkTheme by userPreferences.darkMode.collectAsState(initial = false)
            val language by userPreferences.language.collectAsState(initial = "es")

            // Aplicar idioma al contexto
            val locale = when (language) {
                "en" -> Locale.ENGLISH
                "pt" -> Locale("pt", "BR")
                else -> Locale("es", "PE")
            }
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            createConfigurationContext(config)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)

            HomefixTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}