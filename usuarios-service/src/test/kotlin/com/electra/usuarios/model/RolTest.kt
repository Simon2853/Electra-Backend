package com.electra.usuarios.model

import com.electra.usuarios.dto.aResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas de los roles y de la conversión de la entidad a la respuesta JSON. */
class RolTest {

    @Test
    fun `desdeClave reconoce las claves que usa la app`() {
        assertEquals(Rol.TECNICO, Rol.desdeClave("tecnico"))
        assertEquals(Rol.SUPERVISOR, Rol.desdeClave("supervisor"))
        assertEquals(Rol.ADMINISTRADOR, Rol.desdeClave("admin"))
        assertNull(Rol.desdeClave("jefe"))
        assertNull(Rol.desdeClave(null))
    }

    @Test
    fun `solo el tecnico ve unicamente sus propios registros`() {
        assertFalse(Rol.TECNICO.veRegistrosDeTodos)
        assertTrue(Rol.SUPERVISOR.veRegistrosDeTodos)
        assertTrue(Rol.ADMINISTRADOR.veRegistrosDeTodos)
    }

    @Test
    fun `la respuesta usa la clave del rol y nunca expone el hash`() {
        val usuario = Usuario(id = 7, usuario = "ana", nombre = "Ana", hashContrasena = "secreto", rol = Rol.SUPERVISOR, creadaEn = 5)

        val respuesta = usuario.aResponse()

        assertEquals("supervisor", respuesta.rol)
        assertTrue(respuesta.veRegistrosDeTodos)
        assertEquals(7, respuesta.id)
        assertFalse(respuesta.toString().contains("secreto"))
    }
}
