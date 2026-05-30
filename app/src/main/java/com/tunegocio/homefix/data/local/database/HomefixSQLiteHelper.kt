package com.tunegocio.homefix.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Manejador principal de la base de datos SQLite local
class HomefixSQLiteHelper(context: Context) :
    SQLiteOpenHelper(context, "homefix.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // Tabla usuario — perfil del usuario logueado
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS usuario (
                uid TEXT PRIMARY KEY,
                nombres TEXT DEFAULT '',
                apellidos TEXT DEFAULT '',
                email TEXT DEFAULT '',
                telefono TEXT DEFAULT '',
                distrito TEXT DEFAULT '',
                rol TEXT DEFAULT '',
                selfie_url TEXT DEFAULT '',
                calificacion REAL DEFAULT 0,
                especialidades TEXT DEFAULT '',
                anos_experiencia INTEGER DEFAULT 0,
                es_activo INTEGER DEFAULT 0,
                bio TEXT DEFAULT '',
                lat REAL DEFAULT 0,
                lng REAL DEFAULT 0,
                ultima_actualizacion INTEGER DEFAULT 0
            )
        """.trimIndent())

        // Tabla solicitudes — solicitudes activas
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS solicitudes (
                request_id TEXT PRIMARY KEY,
                client_id TEXT DEFAULT '',
                technician_id TEXT DEFAULT '',
                tipo_servicio TEXT DEFAULT '',
                descripcion TEXT DEFAULT '',
                direccion TEXT DEFAULT '',
                referencia TEXT DEFAULT '',
                distrito TEXT DEFAULT '',
                estado TEXT DEFAULT 'pendiente',
                es_urgente INTEGER DEFAULT 0,
                imagen_url TEXT DEFAULT '',
                lat REAL DEFAULT 0,
                lng REAL DEFAULT 0,
                creado_en INTEGER DEFAULT 0,
                actualizado_en INTEGER DEFAULT 0
            )
        """.trimIndent())

        // Tabla historial — servicios completados o cancelados
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS historial (
                request_id TEXT PRIMARY KEY,
                tipo_servicio TEXT DEFAULT '',
                descripcion TEXT DEFAULT '',
                distrito TEXT DEFAULT '',
                estado_final TEXT DEFAULT '',
                cliente_nombre TEXT DEFAULT '',
                tecnico_nombre TEXT DEFAULT '',
                calificacion_dada INTEGER DEFAULT 0,
                comentario_dado TEXT DEFAULT '',
                creado_en INTEGER DEFAULT 0,
                completado_en INTEGER DEFAULT 0
            )
        """.trimIndent())

        // Tabla notificaciones — historial de notificaciones recibidas
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS notificaciones (
                notificacion_id TEXT PRIMARY KEY,
                titulo TEXT DEFAULT '',
                mensaje TEXT DEFAULT '',
                tipo TEXT DEFAULT '',
                request_id TEXT DEFAULT '',
                leida INTEGER DEFAULT 0,
                creado_en INTEGER DEFAULT 0
            )
        """.trimIndent())

        // Tabla configuracion — preferencias locales del usuario
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS configuracion (
                id INTEGER PRIMARY KEY DEFAULT 1,
                modo_oscuro INTEGER DEFAULT 0,
                idioma TEXT DEFAULT 'es',
                sonido_notificaciones INTEGER DEFAULT 1,
                vibracion_notificaciones INTEGER DEFAULT 1
            )
        """.trimIndent())

        // Insertar fila inicial de configuracion
        db.execSQL("INSERT OR IGNORE INTO configuracion (id) VALUES (1)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Al actualizar version, recrear tablas
        db.execSQL("DROP TABLE IF EXISTS usuario")
        db.execSQL("DROP TABLE IF EXISTS solicitudes")
        db.execSQL("DROP TABLE IF EXISTS historial")
        db.execSQL("DROP TABLE IF EXISTS notificaciones")
        db.execSQL("DROP TABLE IF EXISTS configuracion")
        onCreate(db)
    }
}