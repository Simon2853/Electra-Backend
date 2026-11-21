package com.electra.pruebas.controller

import com.electra.pruebas.dto.PruebaRequest
import com.electra.pruebas.dto.PruebaResponse
import com.electra.pruebas.dto.aResponse
import com.electra.pruebas.service.PruebaService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Pruebas eléctricas")
@RestController
@RequestMapping("/api/pruebas")
class PruebaController(private val servicio: PruebaService) {

    @Operation(summary = "Registrar una prueba")
    @ApiResponse(responseCode = "201", description = "Prueba registrada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o el creador no existe")
    @ApiResponse(responseCode = "403", description = "El rol del creador no puede registrar pruebas")
    @ApiResponse(responseCode = "503", description = "usuarios-service no está disponible")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registrar(@Valid @RequestBody datos: PruebaRequest): PruebaResponse =
        servicio.registrar(datos).aResponse()

    @Operation(summary = "Listar las pruebas visibles para el usuario", description = "Con equipo, entrega el historial de ese equipo.")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    @GetMapping
    fun listar(
        @Parameter(description = "Login de quien consulta", example = "camila@duocuc.cl") @RequestParam usuario: String,
        @Parameter(description = "Código de equipo (opcional)", example = "TR-001") @RequestParam(required = false) equipo: String?
    ): List<PruebaResponse> = servicio.listar(usuario, equipo).map { it.aResponse() }

    @Operation(summary = "Códigos de los equipos que tienen pruebas visibles para el usuario")
    @GetMapping("/equipos")
    fun equipos(@RequestParam usuario: String): List<String> = servicio.equiposConRegistros(usuario)

    @Operation(summary = "Obtener una prueba")
    @ApiResponse(responseCode = "404", description = "No existe o el usuario no puede verla")
    @GetMapping("/{id}")
    fun obtener(@PathVariable id: Long, @RequestParam usuario: String): PruebaResponse =
        servicio.buscar(id, usuario).aResponse()

    @Operation(summary = "Eliminar una prueba", description = "Solo quien la registró o un administrador.")
    @ApiResponse(responseCode = "204", description = "Prueba eliminada")
    @ApiResponse(responseCode = "403", description = "No tiene permiso para eliminarla")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(@PathVariable id: Long, @RequestParam usuario: String) = servicio.eliminar(id, usuario)
}
