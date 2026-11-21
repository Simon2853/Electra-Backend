package com.electra.pruebas.dto

import com.electra.pruebas.model.Prueba
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** Datos que envía la app al registrar una prueba. id y creadoEn los asigna el servidor. */
data class PruebaRequest(
    @field:Schema(example = "TR-001")
    @field:NotBlank(message = "El equipo es obligatorio")
    @field:Size(max = 20, message = "El código de equipo admite hasta 20 caracteres")
    val equipo: String,

    @field:Schema(example = "Resistencia de aislamiento")
    @field:NotBlank(message = "El tipo de prueba es obligatorio")
    @field:Size(max = 80, message = "El tipo de prueba admite hasta 80 caracteres")
    val tipoPrueba: String,

    @field:Schema(example = "21/11/2026")
    @field:Pattern(regexp = "\\d{2}/\\d{2}/\\d{4}", message = "La fecha debe tener el formato dd/MM/aaaa")
    val fecha: String,

    @field:Schema(example = "14:30")
    @field:Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "La hora debe tener el formato HH:mm")
    val hora: String,

    @field:Schema(example = "125.5")
    @field:Pattern(regexp = "-?\\d+(\\.\\d+)?", message = "La medición debe ser un número (ej: 125.5)")
    val medicion: String,

    @field:Schema(example = "MΩ")
    @field:NotBlank(message = "La unidad es obligatoria")
    @field:Size(max = 10, message = "La unidad admite hasta 10 caracteres")
    val unidad: String,

    @field:Schema(example = "Aprobado")
    @field:Pattern(regexp = "Aprobado|Rechazado", message = "El resultado debe ser Aprobado o Rechazado")
    val resultado: String,

    @field:Size(max = 1000, message = "Las observaciones admiten hasta 1000 caracteres")
    val observaciones: String = "",

    @field:Schema(description = "URL de la foto entregada por archivos-service")
    @field:Size(max = 500, message = "La URL de la foto admite hasta 500 caracteres")
    val fotoUrl: String? = null,

    @field:DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @field:DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    val latitud: Double? = null,

    @field:DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @field:DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    val longitud: Double? = null,

    @field:Schema(description = "Login de quien registra; se comprueba en usuarios-service", example = "camila@duocuc.cl")
    @field:NotBlank(message = "El creador es obligatorio")
    val creadoPor: String
)

data class PruebaResponse(
    val id: Long,
    val equipo: String,
    val tipoPrueba: String,
    val fecha: String,
    val hora: String,
    val medicion: String,
    val unidad: String,
    val resultado: String,
    val observaciones: String,
    val fotoUrl: String?,
    val latitud: Double?,
    val longitud: Double?,
    val creadoPor: String,
    @field:Schema(description = "Momento del registro en epoch milisegundos")
    val creadoEn: Long
)

fun Prueba.aResponse() = PruebaResponse(
    id = id ?: 0,
    equipo = equipo,
    tipoPrueba = tipoPrueba,
    fecha = fecha,
    hora = hora,
    medicion = medicion,
    unidad = unidad,
    resultado = resultado,
    observaciones = observaciones,
    fotoUrl = fotoUrl,
    latitud = latitud,
    longitud = longitud,
    creadoPor = creadoPor,
    creadoEn = creadoEn
)
