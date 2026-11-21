package com.electra.archivos.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException

/** Cuerpo de toda respuesta de error: la app muestra [mensaje]. */
data class RespuestaError(val estado: Int, val mensaje: String)

/** Convierte las excepciones en respuestas JSON con el código HTTP correcto. */
@RestControllerAdvice
class ManejadorErrores {

    @ExceptionHandler(ErrorNegocio::class)
    fun negocio(e: ErrorNegocio): ResponseEntity<RespuestaError> = responder(e.estado, e.message.orEmpty())

    /** Spring corta la subida antes de llegar al controlador si supera spring.servlet.multipart.max-file-size. */
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun muyGrande(e: MaxUploadSizeExceededException): ResponseEntity<RespuestaError> =
        responder(HttpStatus.CONTENT_TOO_LARGE, "La imagen supera el tamaño máximo de 5 MB")

    @ExceptionHandler(MissingServletRequestPartException::class)
    fun faltaArchivo(e: MissingServletRequestPartException): ResponseEntity<RespuestaError> =
        responder(HttpStatus.BAD_REQUEST, "Falta la parte ${e.requestPartName} con la imagen")

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun faltaParametro(e: MissingServletRequestParameterException): ResponseEntity<RespuestaError> =
        responder(HttpStatus.BAD_REQUEST, "Falta el parámetro ${e.parameterName}")

    private fun responder(estado: HttpStatus, mensaje: String) =
        ResponseEntity.status(estado).body(RespuestaError(estado.value(), mensaje))
}
