package com.electra.protocolos.controller

import com.electra.protocolos.client.PruebaRemota
import com.electra.protocolos.dto.EstadoRequest
import com.electra.protocolos.dto.ObservacionesRequest
import com.electra.protocolos.dto.ProtocoloRequest
import com.electra.protocolos.dto.ProtocoloResponse
import com.electra.protocolos.dto.SiguienteNumeroResponse
import com.electra.protocolos.dto.aResponse
import com.electra.protocolos.service.ProtocoloService
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Protocolos")
@RestController
@RequestMapping("/api/protocolos")
class ProtocoloController(private val servicio: ProtocoloService) {

    @Operation(summary = "Crear un protocolo", description = "El servidor asigna el número correlativo y el estado inicial.")
    @ApiResponse(responseCode = "201", description = "Protocolo creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o el creador no existe")
    @ApiResponse(responseCode = "403", description = "El rol del creador no puede crear protocolos")
    @ApiResponse(responseCode = "503", description = "usuarios-service no está disponible")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(@Valid @RequestBody datos: ProtocoloRequest): ProtocoloResponse = servicio.crear(datos).aResponse()

    @Operation(summary = "Listar los protocolos visibles para el usuario")
    @GetMapping
    fun listar(
        @Parameter(description = "Login de quien consulta", example = "camila@duocuc.cl") @RequestParam usuario: String
    ): List<ProtocoloResponse> = servicio.listar(usuario).map { it.aResponse() }

    @Operation(summary = "Número que tendría el próximo protocolo (referencial)")
    @GetMapping("/siguiente-numero")
    fun siguienteNumero(): SiguienteNumeroResponse = SiguienteNumeroResponse(servicio.siguienteNumero())

    @Operation(summary = "Obtener un protocolo")
    @ApiResponse(responseCode = "404", description = "No existe o el usuario no puede verlo")
    @GetMapping("/{numero}")
    fun obtener(
        @Parameter(example = "000123") @PathVariable numero: String,
        @RequestParam usuario: String
    ): ProtocoloResponse = servicio.buscar(numero, usuario).aResponse()

    @Operation(summary = "Pruebas del equipo del protocolo (las entrega pruebas-service)")
    @GetMapping("/{numero}/pruebas")
    fun pruebas(@PathVariable numero: String, @RequestParam usuario: String): List<PruebaRemota> =
        servicio.pruebasDelProtocolo(numero, usuario)

    @Operation(summary = "Cambiar el estado (formalizar)")
    @ApiResponse(responseCode = "403", description = "El rol no puede formalizar")
    @ApiResponse(responseCode = "409", description = "El protocolo ya está formalizado")
    @PutMapping("/{numero}/estado")
    fun cambiarEstado(@PathVariable numero: String, @Valid @RequestBody datos: EstadoRequest): ProtocoloResponse =
        servicio.cambiarEstado(numero, datos.estado, datos.usuario).aResponse()

    @Operation(summary = "Editar las observaciones")
    @ApiResponse(responseCode = "409", description = "El protocolo ya está formalizado")
    @PutMapping("/{numero}/observaciones")
    fun actualizarObservaciones(@PathVariable numero: String, @Valid @RequestBody datos: ObservacionesRequest): ProtocoloResponse =
        servicio.actualizarObservaciones(numero, datos.observaciones, datos.usuario).aResponse()

    @Operation(summary = "Eliminar un protocolo no formalizado", description = "Solo quien lo creó o un administrador.")
    @ApiResponse(responseCode = "204", description = "Protocolo eliminado")
    @ApiResponse(responseCode = "403", description = "No tiene permiso")
    @ApiResponse(responseCode = "409", description = "El protocolo ya está formalizado")
    @DeleteMapping("/{numero}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(@PathVariable numero: String, @RequestParam usuario: String) = servicio.eliminar(numero, usuario)
}
