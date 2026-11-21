package com.electra.usuarios.dto

import com.electra.usuarios.model.Usuario
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// Lo que la app envía (Request) y recibe (Response). Las validaciones se revisan antes de llegar
// al servicio; si fallan se responde 400 con el error de cada campo.

data class RegistroRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    @field:Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    val nombre: String,

    @field:NotBlank(message = "El usuario es obligatorio")
    @field:Size(max = 100, message = "El usuario admite hasta 100 caracteres")
    val usuario: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(min = 6, max = 64, message = "La contraseña debe tener entre 6 y 64 caracteres")
    val contrasena: String,

    @field:Schema(description = "tecnico, supervisor o admin; si se omite queda como tecnico", example = "tecnico")
    val rol: String? = null
)

data class LoginRequest(
    @field:NotBlank(message = "El usuario es obligatorio")
    val usuario: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    val contrasena: String
)

data class RestablecerRequest(
    @field:NotBlank(message = "El usuario es obligatorio")
    val usuario: String,

    @field:Schema(description = "Debe coincidir con el nombre registrado (comprobación de identidad)")
    @field:NotBlank(message = "El nombre es obligatorio")
    val nombre: String,

    @field:NotBlank(message = "La contraseña nueva es obligatoria")
    @field:Size(min = 6, max = 64, message = "La contraseña debe tener entre 6 y 64 caracteres")
    val nueva: String
)

data class CambiarContrasenaRequest(
    @field:NotBlank(message = "La contraseña actual es obligatoria")
    val actual: String,

    @field:NotBlank(message = "La contraseña nueva es obligatoria")
    @field:Size(min = 6, max = 64, message = "La contraseña debe tener entre 6 y 64 caracteres")
    val nueva: String
)

data class NombreRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    @field:Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    val nombre: String
)

data class RolRequest(
    @field:Schema(example = "supervisor")
    @field:NotBlank(message = "El rol es obligatorio")
    val rol: String
)

data class AvatarDto(
    @field:Size(max = 20, message = "El color admite hasta 20 caracteres")
    val color: String? = null,

    @field:Size(max = 20, message = "El fondo admite hasta 20 caracteres")
    val fondo: String? = null,

    @field:Schema(description = "URL de la foto entregada por archivos-service")
    @field:Size(max = 500, message = "La URL de la foto admite hasta 500 caracteres")
    val foto: String? = null
)

/** Datos públicos de una cuenta: nunca incluye la contraseña ni su hash. */
data class UsuarioResponse(
    val id: Long,
    val usuario: String,
    val nombre: String,

    @field:Schema(example = "tecnico")
    val rol: String,

    @field:Schema(description = "true si el rol puede ver pruebas y protocolos de otros usuarios")
    val veRegistrosDeTodos: Boolean,

    val avatar: AvatarDto,

    @field:Schema(description = "Creación de la cuenta en epoch milisegundos")
    val creadaEn: Long
)

fun Usuario.aResponse() = UsuarioResponse(
    id = id ?: 0,
    usuario = usuario,
    nombre = nombre,
    rol = rol.clave,
    veRegistrosDeTodos = rol.veRegistrosDeTodos,
    avatar = AvatarDto(avatarColor, avatarFondo, avatarFoto),
    creadaEn = creadaEn
)
