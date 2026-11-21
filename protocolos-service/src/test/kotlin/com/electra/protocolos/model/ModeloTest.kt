package com.electra.protocolos.model

import com.electra.protocolos.Datos
import com.electra.protocolos.dto.aResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas del número correlativo, la visibilidad por rol, los permisos y la conversión a JSON. */
class ModeloTest {

    @Test
    fun `el numero se forma con el id y seis digitos`() {
        assertEquals("#000001", NumeroProtocolo.desdeId(1))
        assertEquals("#000123", NumeroProtocolo.desdeId(123))
        assertEquals("#1234567", NumeroProtocolo.desdeId(1_234_567))
    }

    @Test
    fun `normalizar acepta el numero con o sin almohadilla y ceros`() {
        assertEquals("#000123", NumeroProtocolo.normalizar("123"))
        assertEquals("#000123", NumeroProtocolo.normalizar("000123"))
        assertEquals("#000123", NumeroProtocolo.normalizar(" #000123 "))
        assertNull(NumeroProtocolo.normalizar("abc"))
        assertNull(NumeroProtocolo.normalizar("0"))
    }

    @Test
    fun `el tecnico ve solo lo suyo y quien ve todo desde la creacion de su cuenta`() {
        val tecnico = Alcance("camila", veTodo = false, desde = 0)
        val supervisor = Alcance("jefe", veTodo = true, desde = 500)

        assertEquals("camila", tecnico.creador)
        assertNull(tecnico.creadosDesde)
        assertTrue(tecnico.puedeVer(Datos.protocolo(creadoPor = "camila")))
        assertFalse(tecnico.puedeVer(Datos.protocolo(creadoPor = "otro")))

        assertNull(supervisor.creador)
        assertEquals(500, supervisor.creadosDesde)
        assertTrue(supervisor.puedeVer(Datos.protocolo(creadoPor = "otro", creadoEn = 500)))
        assertFalse(supervisor.puedeVer(Datos.protocolo(creadoPor = "otro", creadoEn = 499)))
    }

    @Test
    fun `permisos por rol iguales a los de la app`() {
        assertTrue(Datos.tecnico.puedeCrearProtocolos)
        assertFalse(Datos.tecnico.puedeFormalizar)
        assertFalse(Datos.supervisor.puedeCrearProtocolos)
        assertTrue(Datos.supervisor.puedeFormalizar)
        assertTrue(Datos.admin.puedeCrearProtocolos)
        assertTrue(Datos.admin.puedeFormalizar)
    }

    @Test
    fun `la respuesta copia los campos y marca el formalizado`() {
        val protocolo = Datos.protocolo(estado = EstadoProtocolo.FORMALIZADO).apply { formalizadoPor = "jefe"; formalizadoEn = 9 }

        val respuesta = protocolo.aResponse()

        assertTrue(protocolo.formalizado)
        assertEquals("#000123", respuesta.numero)
        assertEquals("Formalizado", respuesta.estado)
        assertEquals("jefe", respuesta.formalizadoPor)
        assertEquals(9, respuesta.formalizadoEn)
    }
}
