package com.electra.archivos.controller

import com.electra.archivos.dto.ArchivoResponse
import com.electra.archivos.dto.aResponse
import com.electra.archivos.service.ArchivoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.TimeUnit

@Tag(name = "Archivos")
@RestController
@RequestMapping("/api/archivos")
class ArchivoController(private val servicio: ArchivoService) {

    @Operation(summary = "Subir una imagen", description = "multipart/form-data con la parte \"archivo\". Máximo 5 MB.")
    @ApiResponse(responseCode = "201", description = "Imagen guardada; la respuesta trae su ruta")
    @ApiResponse(responseCode = "400", description = "Imagen vacía, categoría desconocida o falta el usuario")
    @ApiResponse(responseCode = "413", description = "Supera los 5 MB")
    @ApiResponse(responseCode = "415", description = "No es JPEG, PNG ni WebP")
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun subir(
        @RequestPart("archivo") archivo: MultipartFile,
        @Parameter(description = "evidencia o perfil", example = "evidencia") @RequestParam categoria: String,
        @Parameter(description = "Login de quien sube", example = "camila@duocuc.cl") @RequestParam subidoPor: String
    ): ArchivoResponse = servicio.subir(archivo.bytes, archivo.originalFilename, categoria, subidoPor).aResponse()

    @Operation(summary = "Descargar la imagen", description = "Es la ruta que usa la app con Coil (AsyncImage).")
    @ApiResponse(responseCode = "404", description = "No existe")
    @GetMapping("/{id}")
    fun descargar(@PathVariable id: String): ResponseEntity<ByteArray> {
        val imagen = servicio.descargar(id)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(imagen.archivo.tipoContenido))
            // Una imagen nunca cambia (otra foto = otro id): el teléfono puede guardarla en caché.
            .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
            .body(imagen.bytes)
    }

    @Operation(summary = "Datos de una imagen (sin descargarla)")
    @GetMapping("/{id}/info")
    fun info(@PathVariable id: String): ArchivoResponse = servicio.info(id).aResponse()

    @Operation(summary = "Imágenes subidas por un usuario, de la más reciente a la más antigua")
    @GetMapping
    fun listar(@RequestParam subidoPor: String): List<ArchivoResponse> = servicio.listar(subidoPor).map { it.aResponse() }

    @Operation(summary = "Eliminar una imagen", description = "Solo quien la subió.")
    @ApiResponse(responseCode = "204", description = "Eliminada")
    @ApiResponse(responseCode = "403", description = "No la subió este usuario")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(@PathVariable id: String, @RequestParam usuario: String) = servicio.eliminar(id, usuario)
}
