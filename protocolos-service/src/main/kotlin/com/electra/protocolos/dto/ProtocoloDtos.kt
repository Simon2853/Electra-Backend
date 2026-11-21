package com.electra.protocolos.dto

import com.electra.protocolos.model.Protocolo
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** Datos para crear un protocolo. El número y el estado inicial los asigna el servidor. */
data class ProtocoloRequest(
    @field:Schema(example = "TR-001")
    @field:NotBlank(message = "El equipo es obligatorio")
    @field:Size(max = 20, message = "El código de equipo admite hasta 20 caracteres")
    val equipo: String,

    @field:Schema(example = "21/11/2026")
    @field:Pattern(regexp = "\\d{2}/\\d{2}/\\d{4}", message = "La fecha debe tener el formato dd/MM/aaaa")
    val fecha: String,

    @field:Schema(example = "Camila Rojas")
    @field:NotBlank(message = "El responsable es obligatorio")
    @field:Size(min = 3, max = 100, message = "El responsable debe tener entre 3 y 100 caracteres")
    val responsable: String,

    @field:Size(max = 1000, message = "Las observaciones admiten hasta 1000 caracteres")
    val observaciones: String = "",

    @field:Schema(description = "Login de quien crea; se comprueba en usuarios-service", example = "camila@duocuc.cl")
    @field:NotBlank(message = "El creador es obligatorio")
    val creadoPor: String
)

data class EstadoRequest(
    @field:Schema(example = "Formalizado")
    @field:Pattern(regexp = "Registrado en terreno|Formalizado", message = "El estado debe ser 'Registrado en terreno' o 'Formalizado'")
    val estado: String,

    @field:Schema(description = "Login de quien hace el cambio", example = "jefe@duocuc.cl")
    @field:NotBlank(message = "El usuario es obligatorio")
    val usuario: String
)

data class ObservacionesRequest(
    @field:Size(max = 1000, message = "Las observaciones admiten hasta 1000 caracteres")
    val observaciones: String,

    @field:NotBlank(message = "El usuario es obligatorio")
    val usuario: String
)

data class SiguienteNumeroResponse(
    @field:Schema(description = "Número que tendría el próximo protocolo (referencial: se confirma al guardar)", example = "#000124")
    val numero: String
)

data class ProtocoloResponse(
    val id: Long,
    @field:Schema(example = "#000123")
    val numero: String,
    val equipo: String,
    val fecha: String,
    val estado: String,
    val responsable: String,
    val observaciones: String,
    val creadoPor: String,
    val creadoEn: Long,
    val formalizadoPor: String?,
    val formalizadoEn: Long?
)

fun Protocolo.aResponse() = ProtocoloResponse(
    id = id ?: 0,
    numero = numero.orEmpty(),
    equipo = equipo,
    fecha = fecha,
    estado = estado,
    responsable = responsable,
    observaciones = observaciones,
    creadoPor = creadoPor,
    creadoEn = creadoEn,
    formalizadoPor = formalizadoPor,
    formalizadoEn = formalizadoEn
)
