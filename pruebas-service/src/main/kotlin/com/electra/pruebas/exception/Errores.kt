package com.electra.pruebas.exception

import org.springframework.http.HttpStatus

/** Error de negocio con el código HTTP que le corresponde; lo traduce [ManejadorErrores]. */
open class ErrorNegocio(val estado: HttpStatus, mensaje: String) : RuntimeException(mensaje)

class NoEncontrado(mensaje: String) : ErrorNegocio(HttpStatus.NOT_FOUND, mensaje)
class SolicitudInvalida(mensaje: String) : ErrorNegocio(HttpStatus.BAD_REQUEST, mensaje)
class Prohibido(mensaje: String) : ErrorNegocio(HttpStatus.FORBIDDEN, mensaje)

/** usuarios-service no respondió: el problema no es de la solicitud, sino de otro servicio. */
class ServicioNoDisponible(mensaje: String) : ErrorNegocio(HttpStatus.SERVICE_UNAVAILABLE, mensaje)
