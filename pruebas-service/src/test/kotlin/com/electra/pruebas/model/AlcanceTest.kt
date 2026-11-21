package com.electra.pruebas.model

import com.electra.pruebas.Datos
import com.electra.pruebas.dto.aResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas de la regla de visibilidad (qué pruebas ve cada rol) y de la conversión a JSON. */
class AlcanceTest {

    @Test
    fun `el tecnico filtra por creador y no por fecha`() {
        val alcance = Alcance(usuario = "camila", veTodo = false, desde = 500)

        assertEquals("camila", alcance.creador)
        assertNull(alcance.creadosDesde)
        assertTrue(alcance.puedeVer(Datos.prueba(creadoPor = "camila", creadoEn = 1)))
        assertFalse(alcance.puedeVer(Datos.prueba(creadoPor = "otro")))
    }

    @Test
    fun `quien ve todo filtra solo por la fecha de creacion de su cuenta`() {
        val alcance = Alcance(usuario = "jefe", veTodo = true, desde = 500)

        assertNull(alcance.creador)
        assertEquals(500, alcance.creadosDesde)
        assertTrue(alcance.puedeVer(Datos.prueba(creadoPor = "otro", creadoEn = 500)))
        assertFalse(alcance.puedeVer(Datos.prueba(creadoPor = "otro", creadoEn = 499)))
    }

    @Test
    fun `la respuesta copia todos los campos de la entidad`() {
        val respuesta = Datos.prueba(id = 9, creadoEn = 42).apply { fotoUrl = "http://foto"; latitud = 1.0; longitud = 2.0 }.aResponse()

        assertEquals(9, respuesta.id)
        assertEquals("TR-001", respuesta.equipo)
        assertEquals("http://foto", respuesta.fotoUrl)
        assertEquals(1.0, respuesta.latitud)
        assertEquals(2.0, respuesta.longitud)
        assertEquals(42, respuesta.creadoEn)
    }
}
