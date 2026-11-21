package com.electra.pruebas.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Tabla "pruebas" de la BD electra_pruebas: una medición eléctrica hecha en terreno.
 * Una prueba se registra una vez y no se edita (es evidencia); solo puede eliminarse.
 * [creadoPor] es el login de usuarios-service: se guarda como texto porque cada servicio
 * tiene su propia BD y no hay llaves foráneas entre servicios.
 */
@Entity
@Table(name = "pruebas", indexes = [Index(name = "idx_pruebas_equipo", columnList = "equipo")])
class Prueba(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 20)
    var equipo: String,

    @Column(name = "tipo_prueba", nullable = false, length = 80)
    var tipoPrueba: String,

    @Column(nullable = false, length = 10)
    var fecha: String,

    @Column(nullable = false, length = 5)
    var hora: String,

    @Column(nullable = false, length = 20)
    var medicion: String,

    @Column(nullable = false, length = 10)
    var unidad: String,

    @Column(nullable = false, length = 20)
    var resultado: String,

    @Column(nullable = false, length = 1000)
    var observaciones: String = "",

    /** URL de la foto de evidencia (la entrega archivos-service). */
    @Column(name = "foto_url", length = 500)
    var fotoUrl: String? = null,

    var latitud: Double? = null,

    var longitud: Double? = null,

    @Column(name = "creado_por", nullable = false, length = 100)
    var creadoPor: String,

    /** Momento en que se guardó (epoch ms); lo pone el servidor, no la app. */
    @Column(name = "creado_en", nullable = false)
    var creadoEn: Long
)
