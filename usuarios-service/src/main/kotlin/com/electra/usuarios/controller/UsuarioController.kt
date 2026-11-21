package com.electra.usuarios.controller

import com.electra.usuarios.dto.AvatarDto
import com.electra.usuarios.dto.CambiarContrasenaRequest
import com.electra.usuarios.dto.NombreRequest
import com.electra.usuarios.dto.RolRequest
import com.electra.usuarios.dto.UsuarioResponse
import com.electra.usuarios.dto.aResponse
import com.electra.usuarios.service.UsuarioService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Consulta y edición de cuentas. {usuario} es el login de la cuenta. pruebas-service y
 * protocolos-service usan GET /api/usuarios/{usuario} (con Feign) para comprobar que el creador existe.
 */
@Tag(name = "Usuarios")
@RestController
@RequestMapping("/api/usuarios")
class UsuarioController(private val servicio: UsuarioService) {

    @Operation(summary = "Listar todas las cuentas ordenadas por nombre")
    @GetMapping
    fun listar(): List<UsuarioResponse> = servicio.listar().map { it.aResponse() }

    @Operation(summary = "Obtener una cuenta")
    @ApiResponse(responseCode = "200", description = "Cuenta encontrada")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    @GetMapping("/{usuario}")
    fun obtener(@PathVariable usuario: String): UsuarioResponse = servicio.buscar(usuario).aResponse()

    @Operation(summary = "Cambiar el nombre visible")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    @PutMapping("/{usuario}/nombre")
    fun actualizarNombre(@PathVariable usuario: String, @Valid @RequestBody datos: NombreRequest): UsuarioResponse =
        servicio.actualizarNombre(usuario, datos.nombre).aResponse()

    @Operation(summary = "Cambiar la contraseña conociendo la actual")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada")
    @ApiResponse(responseCode = "401", description = "La contraseña actual no es correcta")
    @PutMapping("/{usuario}/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cambiarContrasena(@PathVariable usuario: String, @Valid @RequestBody datos: CambiarContrasenaRequest) =
        servicio.cambiarContrasena(usuario, datos.actual, datos.nueva)

    @Operation(summary = "Cambiar el avatar (colores y foto de perfil)")
    @PutMapping("/{usuario}/avatar")
    fun actualizarAvatar(@PathVariable usuario: String, @Valid @RequestBody avatar: AvatarDto): UsuarioResponse =
        servicio.actualizarAvatar(usuario, avatar).aResponse()

    @Operation(summary = "Cambiar el rol", description = "En la app solo el administrador ve esta opción.")
    @ApiResponse(responseCode = "400", description = "Rol desconocido")
    @PutMapping("/{usuario}/rol")
    fun actualizarRol(@PathVariable usuario: String, @Valid @RequestBody datos: RolRequest): UsuarioResponse =
        servicio.actualizarRol(usuario, datos.rol).aResponse()
}
