package com.electra.archivos.dto

import com.electra.archivos.model.Archivo
import io.swagger.v3.oas.annotations.media.Schema

/** Datos de un archivo subido. */
data class ArchivoResponse(
    val id: String,

    @field:Schema(
        description = "Ruta relativa para descargar la imagen a través del Gateway. La app le antepone su URL base, " +
            "así la ruta guardada en otros servicios sigue sirviendo aunque cambie la URL pública.",
        example = "/api/archivos/3f2a9c1e-8b7d-4c6a-9e21-5d4f8a7b6c30"
    )
    val ruta: String,

    val nombreOriginal: String,
    @field:Schema(example = "image/jpeg")
    val tipoContenido: String,
    @field:Schema(description = "Tamaño en bytes")
    val tamano: Long,
    @field:Schema(example = "evidencia")
    val categoria: String,
    val subidoPor: String,
    val subidoEn: Long
)

fun Archivo.aResponse() = ArchivoResponse(
    id = id,
    ruta = "/api/archivos/$id",
    nombreOriginal = nombreOriginal,
    tipoContenido = tipoContenido,
    tamano = tamano,
    categoria = categoria.name.lowercase(),
    subidoPor = subidoPor,
    subidoEn = subidoEn
)
