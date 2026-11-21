package com.electra.protocolos.exception

import org.springframework.http.HttpStatus

/** Error de negocio con el código HTTP que le corresponde; lo traduce [ManejadorErrores]. */
open class ErrorNegocio(val estado: HttpStatus, mensaje: String) : RuntimeException(mensaje)

class NoEncontrado(mensaje: String) : ErrorNegocio(HttpStatus.NOT_FOUND, mensaje)
class SolicitudInvalida(mensaje: String) : ErrorNegocio(HttpStatus.BAD_REQUEST, mensaje)
class Prohibido(mensaje: String) : ErrorNegocio(HttpStatus.FORBIDDEN, mensaje)

/** Otro microservicio (usuarios o pruebas) no respondió: el problema no es de la solicitud. */
class ServicioNoDisponible(mensaje: String) : ErrorNegocio(HttpStatus.SERVICE_UNAVAILABLE, mensaje)

/** La acción choca con el estado actual (p. ej. modificar un protocolo ya formalizado). */
class Conflicto(mensaje: String) : ErrorNegocio(HttpStatus.CONFLICT, mensaje)
