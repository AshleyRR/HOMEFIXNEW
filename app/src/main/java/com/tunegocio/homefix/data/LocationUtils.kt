package com.tunegocio.homefix.data

import kotlin.math.*

object LocationUtils {

    // Calcula distancia en km entre 2 puntos pal GPS
    fun haversineDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val earthRadius = 6371.0 // km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    // Formatea la distancia para mostrar en UI
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
            distanceKm < 10.0 -> "${"%.1f".format(distanceKm)} km"
            else -> "${distanceKm.toInt()} km"
        }
    }

    // Coordenadas centrales de cada distrito de Lima
    // Usadas como referencia cuando el técnico no tiene GPS activo
    val districtCoordinates = mapOf(
        "Ancón" to Pair(-11.6089, -77.1647),
        "Ate" to Pair(-12.0261, -76.9194),
        "Barranco" to Pair(-12.1464, -77.0216),
        "Breña" to Pair(-12.0611, -77.0497),
        "Carabayllo" to Pair(-11.8892, -77.0275),
        "Cercado de Lima" to Pair(-12.0464, -77.0428),
        "Chaclacayo" to Pair(-11.9775, -76.7697),
        "Chorrillos" to Pair(-12.1703, -77.0172),
        "Cieneguilla" to Pair(-12.0758, -76.7833),
        "Comas" to Pair(-11.9392, -77.0489),
        "El Agustino" to Pair(-12.0444, -76.9972),
        "Independencia" to Pair(-11.9978, -77.0556),
        "Jesús María" to Pair(-12.0703, -77.0497),
        "La Molina" to Pair(-12.0858, -76.9422),
        "La Victoria" to Pair(-12.0650, -77.0189),
        "Lince" to Pair(-12.0833, -77.0333),
        "Los Olivos" to Pair(-11.9675, -77.0694),
        "Lurigancho" to Pair(-11.9908, -76.8394),
        "Lurín" to Pair(-12.2767, -76.8694),
        "Magdalena del Mar" to Pair(-12.0928, -77.0728),
        "Miraflores" to Pair(-12.1186, -77.0294),
        "Pachacámac" to Pair(-12.2333, -76.8667),
        "Pucusana" to Pair(-12.4833, -76.8000),
        "Pueblo Libre" to Pair(-12.0750, -77.0622),
        "Puente Piedra" to Pair(-11.8667, -77.0753),
        "Punta Hermosa" to Pair(-12.3333, -76.8167),
        "Punta Negra" to Pair(-12.3667, -76.8000),
        "Rímac" to Pair(-12.0317, -77.0322),
        "San Bartolo" to Pair(-12.3833, -76.7833),
        "San Borja" to Pair(-12.1011, -76.9983),
        "San Isidro" to Pair(-12.0975, -77.0428),
        "San Juan de Lurigancho" to Pair(-11.9833, -77.0050),
        "San Juan de Miraflores" to Pair(-12.1575, -76.9736),
        "San Luis" to Pair(-12.0722, -76.9978),
        "San Martín de Porres" to Pair(-12.0089, -77.0747),
        "San Miguel" to Pair(-12.0778, -77.0878),
        "Santa Anita" to Pair(-12.0464, -76.9714),
        "Santa María del Mar" to Pair(-12.4167, -76.7833),
        "Santa Rosa" to Pair(-11.7833, -77.1833),
        "Santiago de Surco" to Pair(-12.1383, -76.9956),
        "Surquillo" to Pair(-12.1111, -77.0183),
        "Villa El Salvador" to Pair(-12.2131, -76.9422),
        "Villa María del Triunfo" to Pair(-12.1686, -76.9419)
    )
}