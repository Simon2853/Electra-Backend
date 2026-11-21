package com.electra.protocolos.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

/** Lo que este servicio necesita de una cuenta (parte de la respuesta de usuarios-service). */
data class UsuarioRemoto(
    val usuario: String,
    val nombre: String,
    val rol: String,
    val veRegistrosDeTodos: Boolean,
    val creadaEn: Long
) {
    /** Mismos permisos que Rol en la app: el técnico crea, el supervisor formaliza, el admin hace todo. */
    val puedeCrearProtocolos: Boolean get() = rol == "tecnico" || rol == "admin"
    val puedeFormalizar: Boolean get() = rol == "supervisor" || rol == "admin"
}

/** Una prueba tal como la entrega pruebas-service (solo los campos que muestra un protocolo). */
data class PruebaRemota(
    val id: Long,
    val equipo: String,
    val tipoPrueba: String,
    val fecha: String,
    val hora: String,
    val medicion: String,
    val unidad: String,
    val resultado: String
)

/** Cliente Feign hacia usuarios-service: se busca por su nombre en Eureka, sin IP ni puerto. */
@FeignClient(name = "usuarios-service")
interface UsuariosClient {

    @GetMapping("/api/usuarios/{usuario}")
    fun obtener(@PathVariable("usuario") usuario: String): UsuarioRemoto
}

/** Cliente Feign hacia pruebas-service: entrega las pruebas del equipo de un protocolo. */
@FeignClient(name = "pruebas-service")
interface PruebasClient {

    @GetMapping("/api/pruebas")
    fun listar(@RequestParam("usuario") usuario: String, @RequestParam("equipo") equipo: String): List<PruebaRemota>
}
