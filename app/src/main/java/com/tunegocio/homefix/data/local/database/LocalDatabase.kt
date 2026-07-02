package com.tunegocio.homefix.data.local.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

// Clase principal para operaciones CRUD en SQLite
class LocalDatabase(context: Context) {
    private val dbHelper = HomefixSQLiteHelper(context)

    // TABLA: usuario
    fun guardarUsuario(
        uid: String,
        nombres: String,
        apellidos: String,
        email: String,
        telefono: String,
        distrito: String,
        rol: String,
        selfieUrl: String,
        calificacion: Float,
        especialidades: String,
        anosExperiencia: Int,
        bio: String,
        lat: Double,
        lng: Double
    ) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("uid", uid)
            put("nombres", nombres)
            put("apellidos", apellidos)
            put("email", email)
            put("telefono", telefono)
            put("distrito", distrito)
            put("rol", rol)
            put("selfie_url", selfieUrl)
            put("calificacion", calificacion)
            put("especialidades", especialidades)
            put("anos_experiencia", anosExperiencia)
            put("bio", bio)
            put("lat", lat)
            put("lng", lng)
            put("ultima_actualizacion", System.currentTimeMillis())
        }
        db.insertWithOnConflict("usuario", null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun obtenerUsuario(uid: String): Map<String, Any>? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            "usuario", null, "uid = ?",
            arrayOf(uid), null, null, null
        )
        var resultado: Map<String, Any>? = null
        if (cursor.moveToFirst()) {
            resultado = mapOf(
                "uid" to cursor.getString(cursor.getColumnIndexOrThrow("uid")),
                "nombres" to cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
                "apellidos" to cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                "email" to cursor.getString(cursor.getColumnIndexOrThrow("email")),
                "telefono" to cursor.getString(cursor.getColumnIndexOrThrow("telefono")),
                "distrito" to cursor.getString(cursor.getColumnIndexOrThrow("distrito")),
                "rol" to cursor.getString(cursor.getColumnIndexOrThrow("rol")),
                "selfieUrl" to cursor.getString(cursor.getColumnIndexOrThrow("selfie_url")),
                "calificacion" to cursor.getFloat(cursor.getColumnIndexOrThrow("calificacion")),
                "especialidades" to cursor.getString(cursor.getColumnIndexOrThrow("especialidades")),
                "anosExperiencia" to cursor.getInt(cursor.getColumnIndexOrThrow("anos_experiencia")),
                "bio" to cursor.getString(cursor.getColumnIndexOrThrow("bio")),
                "lat" to cursor.getDouble(cursor.getColumnIndexOrThrow("lat")),
                "lng" to cursor.getDouble(cursor.getColumnIndexOrThrow("lng"))
            )
        }
        cursor.close()
        db.close()
        return resultado
    }

    fun limpiarUsuario() {
        val db = dbHelper.writableDatabase
        db.delete("usuario", null, null)
        db.close()
    }

    // ══════════════════════════════════════════
    // TABLA: solicitudes
    // ══════════════════════════════════════════

    fun guardarSolicitud(
        requestId: String,
        clientId: String,
        technicianId: String,
        tipoServicio: String,
        descripcion: String,
        direccion: String,
        referencia: String,
        distrito: String,
        estado: String,
        esUrgente: Boolean,
        imagenUrl: String,
        lat: Double,
        lng: Double,
        creadoEn: Long,
        actualizadoEn: Long
    ) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("request_id", requestId)
            put("client_id", clientId)
            put("technician_id", technicianId)
            put("tipo_servicio", tipoServicio)
            put("descripcion", descripcion)
            put("direccion", direccion)
            put("referencia", referencia)
            put("distrito", distrito)
            put("estado", estado)
            put("es_urgente", if (esUrgente) 1 else 0)
            put("imagen_url", imagenUrl)
            put("lat", lat)
            put("lng", lng)
            put("creado_en", creadoEn)
            put("actualizado_en", actualizadoEn)
        }
        db.insertWithOnConflict("solicitudes", null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun obtenerSolicitudesCliente(clientId: String): List<Map<String, Any>> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "solicitudes", null, "client_id = ?",
            arrayOf(clientId), null, null, "creado_en DESC"
        )
        val lista = mutableListOf<Map<String, Any>>()
        while (cursor.moveToNext()) {
            lista.add(mapOf(
                "requestId" to cursor.getString(cursor.getColumnIndexOrThrow("request_id")),
                "clientId" to cursor.getString(cursor.getColumnIndexOrThrow("client_id")),
                "technicianId" to cursor.getString(cursor.getColumnIndexOrThrow("technician_id")),
                "tipoServicio" to cursor.getString(cursor.getColumnIndexOrThrow("tipo_servicio")),
                "descripcion" to cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                "direccion" to cursor.getString(cursor.getColumnIndexOrThrow("direccion")),
                "referencia" to cursor.getString(cursor.getColumnIndexOrThrow("referencia")),
                "distrito" to cursor.getString(cursor.getColumnIndexOrThrow("distrito")),
                "estado" to cursor.getString(cursor.getColumnIndexOrThrow("estado")),
                "esUrgente" to (cursor.getInt(cursor.getColumnIndexOrThrow("es_urgente")) == 1),
                "imagenUrl" to cursor.getString(cursor.getColumnIndexOrThrow("imagen_url")),
                "lat" to cursor.getDouble(cursor.getColumnIndexOrThrow("lat")),
                "lng" to cursor.getDouble(cursor.getColumnIndexOrThrow("lng")),
                "creadoEn" to cursor.getLong(cursor.getColumnIndexOrThrow("creado_en")),
                "actualizadoEn" to cursor.getLong(cursor.getColumnIndexOrThrow("actualizado_en"))
            ))
        }
        cursor.close()
        db.close()
        return lista
    }

    fun obtenerSolicitudesPendientes(): List<Map<String, Any>> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "solicitudes", null, "estado = ?",
            arrayOf("pendiente"), null, null, "creado_en DESC"
        )
        val lista = mutableListOf<Map<String, Any>>()
        while (cursor.moveToNext()) {
            lista.add(mapOf(
                "requestId" to cursor.getString(cursor.getColumnIndexOrThrow("request_id")),
                "tipoServicio" to cursor.getString(cursor.getColumnIndexOrThrow("tipo_servicio")),
                "descripcion" to cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                "distrito" to cursor.getString(cursor.getColumnIndexOrThrow("distrito")),
                "estado" to cursor.getString(cursor.getColumnIndexOrThrow("estado")),
                "esUrgente" to (cursor.getInt(cursor.getColumnIndexOrThrow("es_urgente")) == 1),
                "lat" to cursor.getDouble(cursor.getColumnIndexOrThrow("lat")),
                "lng" to cursor.getDouble(cursor.getColumnIndexOrThrow("lng"))
            ))
        }
        cursor.close()
        db.close()
        return lista
    }

    fun eliminarSolicitud(requestId: String) {
        val db = dbHelper.writableDatabase
        db.delete("solicitudes", "request_id = ?", arrayOf(requestId))
        db.close()
    }

    fun limpiarSolicitudes() {
        val db = dbHelper.writableDatabase
        db.delete("solicitudes", null, null)
        db.close()
    }

    // ══════════════════════════════════════════
    // TABLA: historial
    // ══════════════════════════════════════════

    fun guardarHistorial(
        requestId: String,
        tipoServicio: String,
        descripcion: String,
        distrito: String,
        estadoFinal: String,
        clienteNombre: String,
        tecnicoNombre: String,
        calificacionDada: Int,
        comentarioDado: String,
        creadoEn: Long,
        completadoEn: Long
    ) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("request_id", requestId)
            put("tipo_servicio", tipoServicio)
            put("descripcion", descripcion)
            put("distrito", distrito)
            put("estado_final", estadoFinal)
            put("cliente_nombre", clienteNombre)
            put("tecnico_nombre", tecnicoNombre)
            put("calificacion_dada", calificacionDada)
            put("comentario_dado", comentarioDado)
            put("creado_en", creadoEn)
            put("completado_en", completadoEn)
        }
        db.insertWithOnConflict("historial", null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun obtenerHistorial(): List<Map<String, Any>> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "historial", null, null,
            null, null, null, "creado_en DESC"
        )
        val lista = mutableListOf<Map<String, Any>>()
        while (cursor.moveToNext()) {
            lista.add(mapOf(
                "requestId" to cursor.getString(cursor.getColumnIndexOrThrow("request_id")),
                "tipoServicio" to cursor.getString(cursor.getColumnIndexOrThrow("tipo_servicio")),
                "descripcion" to cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                "distrito" to cursor.getString(cursor.getColumnIndexOrThrow("distrito")),
                "estadoFinal" to cursor.getString(cursor.getColumnIndexOrThrow("estado_final")),
                "clienteNombre" to cursor.getString(cursor.getColumnIndexOrThrow("cliente_nombre")),
                "tecnicoNombre" to cursor.getString(cursor.getColumnIndexOrThrow("tecnico_nombre")),
                "calificacionDada" to cursor.getInt(cursor.getColumnIndexOrThrow("calificacion_dada")),
                "comentarioDado" to cursor.getString(cursor.getColumnIndexOrThrow("comentario_dado")),
                "creadoEn" to cursor.getLong(cursor.getColumnIndexOrThrow("creado_en")),
                "completadoEn" to cursor.getLong(cursor.getColumnIndexOrThrow("completado_en"))
            ))
        }
        cursor.close()
        db.close()
        return lista
    }

    fun limpiarHistorial() {
        val db = dbHelper.writableDatabase
        db.delete("historial", null, null)
        db.close()
    }

    // ══════════════════════════════════════════
    // TABLA: notificaciones
    // ══════════════════════════════════════════

    fun guardarNotificacion(
        notificacionId: String,
        titulo: String,
        mensaje: String,
        tipo: String,
        requestId: String
    ) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("notificacion_id", notificacionId)
            put("titulo", titulo)
            put("mensaje", mensaje)
            put("tipo", tipo)
            put("request_id", requestId)
            put("leida", 0)
            put("creado_en", System.currentTimeMillis())
        }
        db.insertWithOnConflict("notificaciones", null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun obtenerNotificaciones(): List<Map<String, Any>> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "notificaciones", null, null,
            null, null, null, "creado_en DESC"
        )
        val lista = mutableListOf<Map<String, Any>>()
        while (cursor.moveToNext()) {
            lista.add(mapOf(
                "notificacionId" to cursor.getString(cursor.getColumnIndexOrThrow("notificacion_id")),
                "titulo" to cursor.getString(cursor.getColumnIndexOrThrow("titulo")),
                "mensaje" to cursor.getString(cursor.getColumnIndexOrThrow("mensaje")),
                "tipo" to cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                "requestId" to cursor.getString(cursor.getColumnIndexOrThrow("request_id")),
                "leida" to (cursor.getInt(cursor.getColumnIndexOrThrow("leida")) == 1),
                "creadoEn" to cursor.getLong(cursor.getColumnIndexOrThrow("creado_en"))
            ))
        }
        cursor.close()
        db.close()
        return lista
    }

    fun contarNoLeidas(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM notificaciones WHERE leida = 0", null
        )
        var count = 0
        if (cursor.moveToFirst()) count = cursor.getInt(0)
        cursor.close()
        db.close()
        return count
    }

    fun marcarLeida(notificacionId: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("leida", 1) }
        db.update("notificaciones", values, "notificacion_id = ?", arrayOf(notificacionId))
        db.close()
    }

    fun marcarTodasLeidas() {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("leida", 1) }
        db.update("notificaciones", values, null, null)
        db.close()
    }

    fun limpiarNotificacionesAntiguas(timestampLimite: Long) {
        val db = dbHelper.writableDatabase
        db.delete("notificaciones", "creado_en < ?", arrayOf(timestampLimite.toString()))
        db.close()
    }

    // ══════════════════════════════════════════
    // TABLA: configuracion
    // ══════════════════════════════════════════

    fun obtenerConfiguracion(): Map<String, Any> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "configuracion", null, "id = 1",
            null, null, null, null
        )
        var resultado = mapOf<String, Any>(
            "modoOscuro" to false,
            "idioma" to "es",
            "sonidoNotificaciones" to true,
            "vibracionNotificaciones" to true
        )
        if (cursor.moveToFirst()) {
            resultado = mapOf(
                "modoOscuro" to (cursor.getInt(cursor.getColumnIndexOrThrow("modo_oscuro")) == 1),
                "idioma" to cursor.getString(cursor.getColumnIndexOrThrow("idioma")),
                "sonidoNotificaciones" to (cursor.getInt(cursor.getColumnIndexOrThrow("sonido_notificaciones")) == 1),
                "vibracionNotificaciones" to (cursor.getInt(cursor.getColumnIndexOrThrow("vibracion_notificaciones")) == 1)
            )
        }
        cursor.close()
        db.close()
        return resultado
    }

    fun actualizarModoOscuro(valor: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("modo_oscuro", if (valor) 1 else 0) }
        db.update("configuracion", values, "id = 1", null)
        db.close()
    }

    fun actualizarIdioma(valor: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("idioma", valor) }
        db.update("configuracion", values, "id = 1", null)
        db.close()
    }

    fun actualizarSonido(valor: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("sonido_notificaciones", if (valor) 1 else 0) }
        db.update("configuracion", values, "id = 1", null)
        db.close()
    }

    fun actualizarVibracion(valor: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("vibracion_notificaciones", if (valor) 1 else 0) }
        db.update("configuracion", values, "id = 1", null)
        db.close()
    }
}