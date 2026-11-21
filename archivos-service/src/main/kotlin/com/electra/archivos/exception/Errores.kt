package com.electra.archivos.exception

import org.springframework.http.HttpStatus

/** Error de negocio con el código HTTP que le corresponde; lo traduce [ManejadorErrores]. */
open class ErrorNegocio(val estado: HttpStatus, mensaje: String) : RuntimeException(mensaje)

class NoEncontrado(mensaje: String) : ErrorNegocio(HttpStatus.NOT_FOUND, mensaje)
class SolicitudInvalida(mensaje: String) : ErrorNegocio(HttpStatus.BAD_REQUEST, mensaje)
class Prohibido(mensaje: String) : ErrorNegocio(HttpStatus.FORBIDDEN, mensaje)

/** El archivo no es una imagen JPEG, PNG o WebP. */
class TipoNoSoportado(mensaje: String) : ErrorNegocio(HttpStatus.UNSUPPORTED_MEDIA_TYPE, mensaje)
