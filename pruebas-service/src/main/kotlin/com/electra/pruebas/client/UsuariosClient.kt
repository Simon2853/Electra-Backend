package com.electra.pruebas.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

/** Lo que este servicio necesita saber de una cuenta (es parte de la respuesta de usuarios-service). */
data class UsuarioRemoto(
    val usuario: String,
    val rol: String,
    val veRegistrosDeTodos: Boolean,
    val creadaEn: Long
)

/**
 * Cliente Feign: Spring genera la implementación de esta interfaz. "usuarios-service" es el nombre
 * con que ese servicio se registra en Eureka, así que no se escribe ninguna IP ni puerto.
 */
@FeignClient(name = "usuarios-service")
interface UsuariosClient {

    @GetMapping("/api/usuarios/{usuario}")
    fun obtener(@PathVariable("usuario") usuario: String): UsuarioRemoto
}
