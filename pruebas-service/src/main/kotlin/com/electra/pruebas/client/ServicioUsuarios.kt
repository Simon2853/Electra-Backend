package com.electra.pruebas.client

import com.electra.pruebas.exception.NoEncontrado
import com.electra.pruebas.exception.ServicioNoDisponible
import com.electra.pruebas.model.Alcance
import feign.FeignException
import org.springframework.stereotype.Component

/**
 * Envuelve las llamadas Feign a usuarios-service y traduce sus fallas a errores de este servicio:
 * - 404 allá  -> el usuario no existe (NoEncontrado);
 * - cualquier otra falla (caído, sin instancias en Eureka, tiempo agotado) -> 503.
 */
@Component
class ServicioUsuarios(private val cliente: UsuariosClient) {

    fun obtener(usuario: String): UsuarioRemoto =
        try {
            cliente.obtener(usuario.trim())
        } catch (e: FeignException) {
            if (e.status() == 404) throw NoEncontrado("No existe el usuario ${usuario.trim()}")
            throw ServicioNoDisponible("usuarios-service no está disponible; intente nuevamente")
        }

    /** Alcance de quien consulta, según su rol y la fecha de creación de su cuenta. */
    fun alcanceDe(usuario: String): Alcance {
        val cuenta = obtener(usuario)
        return Alcance(usuario = cuenta.usuario, veTodo = cuenta.veRegistrosDeTodos, desde = cuenta.creadaEn)
    }
}
