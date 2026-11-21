package com.electra.usuarios.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Cuerpo de toda respuesta de error: la app muestra [mensaje] y, si hay, el error de cada campo. */
data class RespuestaError(
    val estado: Int,
    val mensaje: String,
    val campos: Map<String, String> = emptyMap()
)

/** Convierte las excepciones en respuestas JSON con el código HTTP correcto (400, 401, 404, 409). */
@RestControllerAdvice
class ManejadorErrores {

    @ExceptionHandler(ErrorNegocio::class)
    fun negocio(e: ErrorNegocio): ResponseEntity<RespuestaError> =
        ResponseEntity.status(e.estado).body(RespuestaError(e.estado.value(), e.message.orEmpty()))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(e: MethodArgumentNotValidException): ResponseEntity<RespuestaError> {
        val campos = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor inválido") }
        return ResponseEntity.badRequest()
            .body(RespuestaError(HttpStatus.BAD_REQUEST.value(), "Datos inválidos", campos))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun cuerpoIlegible(e: HttpMessageNotReadableException): ResponseEntity<RespuestaError> =
        ResponseEntity.badRequest()
            .body(RespuestaError(HttpStatus.BAD_REQUEST.value(), "El cuerpo de la solicitud no es un JSON válido"))
}
