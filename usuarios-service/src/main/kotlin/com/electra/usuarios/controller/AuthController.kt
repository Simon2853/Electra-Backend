package com.electra.usuarios.controller

import com.electra.usuarios.dto.LoginRequest
import com.electra.usuarios.dto.RegistroRequest
import com.electra.usuarios.dto.RestablecerRequest
import com.electra.usuarios.dto.UsuarioResponse
import com.electra.usuarios.dto.aResponse
import com.electra.usuarios.service.UsuarioService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Acciones previas a tener sesión: crear cuenta, iniciar sesión y recuperar la contraseña. */
@Tag(name = "Autenticación")
@RestController
@RequestMapping("/api/auth")
class AuthController(private val servicio: UsuarioService) {

    @Operation(summary = "Crear una cuenta")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o rol desconocido")
    @ApiResponse(responseCode = "409", description = "El usuario ya existe")
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    fun registrar(@Valid @RequestBody datos: RegistroRequest): UsuarioResponse =
        servicio.registrar(datos).aResponse()

    @Operation(summary = "Iniciar sesión", description = "Devuelve los datos de la cuenta, incluido su rol.")
    @ApiResponse(responseCode = "200", description = "Credenciales correctas")
    @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos")
    @PostMapping("/login")
    fun iniciarSesion(@Valid @RequestBody datos: LoginRequest): UsuarioResponse =
        servicio.iniciarSesion(datos.usuario, datos.contrasena).aResponse()

    @Operation(summary = "Restablecer la contraseña olvidada")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada")
    @ApiResponse(responseCode = "400", description = "El nombre no coincide")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    @PostMapping("/restablecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun restablecer(@Valid @RequestBody datos: RestablecerRequest) =
        servicio.restablecer(datos.usuario, datos.nombre, datos.nueva)
}
