package com.electra.usuarios.exception

import org.springframework.http.HttpStatus

/** Error de negocio con el código HTTP que le corresponde; lo traduce [ManejadorErrores]. */
open class ErrorNegocio(val estado: HttpStatus, mensaje: String) : RuntimeException(mensaje)

class NoEncontrado(mensaje: String) : ErrorNegocio(HttpStatus.NOT_FOUND, mensaje)
class Conflicto(mensaje: String) : ErrorNegocio(HttpStatus.CONFLICT, mensaje)
class NoAutorizado(mensaje: String) : ErrorNegocio(HttpStatus.UNAUTHORIZED, mensaje)
class SolicitudInvalida(mensaje: String) : ErrorNegocio(HttpStatus.BAD_REQUEST, mensaje)
