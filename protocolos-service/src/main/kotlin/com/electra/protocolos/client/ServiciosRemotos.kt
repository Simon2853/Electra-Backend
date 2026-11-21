package com.electra.protocolos.client

import com.electra.protocolos.exception.NoEncontrado
import com.electra.protocolos.exception.ServicioNoDisponible
import com.electra.protocolos.model.Alcance
import feign.FeignException
import org.springframework.stereotype.Component

/**
 * Envuelve las llamadas Feign y traduce sus fallas a errores de este servicio:
 * un 404 allá significa "no existe"; cualquier otra falla (caído, sin instancias, tiempo agotado) es 503.
 */
@Component
class ServiciosRemotos(
    private val usuarios: UsuariosClient,
    private val pruebas: PruebasClient
) {

    fun usuario(login: String): UsuarioRemoto =
        llamar("usuarios-service", siNoExiste = "No existe el usuario ${login.trim()}") { usuarios.obtener(login.trim()) }

    fun alcanceDe(login: String): Alcance {
        val cuenta = usuario(login)
        return Alcance(usuario = cuenta.usuario, veTodo = cuenta.veRegistrosDeTodos, desde = cuenta.creadaEn)
    }

    /** Pruebas del equipo que puede ver [login] (pruebas-service aplica su propio alcance). */
    fun pruebasDe(login: String, equipo: String): List<PruebaRemota> =
        llamar("pruebas-service", siNoExiste = "No existe el usuario ${login.trim()}") { pruebas.listar(login.trim(), equipo) }

    private fun <T> llamar(servicio: String, siNoExiste: String, llamada: () -> T): T =
        try {
            llamada()
        } catch (e: FeignException) {
            if (e.status() == 404) throw NoEncontrado(siNoExiste)
            throw ServicioNoDisponible("$servicio no está disponible; intente nuevamente")
        }
}
