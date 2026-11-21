package com.electra.archivos.model

import com.electra.archivos.Datos
import com.electra.archivos.dto.aResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pruebas de la detección del formato por el contenido real del archivo. */
class FormatoImagenTest {

    @Test
    fun `reconoce JPEG, PNG y WebP por sus primeros bytes`() {
        assertEquals(FormatoImagen.JPEG, FormatoImagen.detectar(Datos.jpeg))
        assertEquals(FormatoImagen.PNG, FormatoImagen.detectar(Datos.png))
        assertEquals(FormatoImagen.WEBP, FormatoImagen.detectar(Datos.webp))
    }

    @Test
    fun `rechaza lo que no es imagen aunque sea muy corto`() {
        assertNull(FormatoImagen.detectar(Datos.texto))
        assertNull(FormatoImagen.detectar(byteArrayOf(0xFF.toByte())))
        assertNull(FormatoImagen.detectar("RIFF0000WAVE".toByteArray())) // RIFF de audio, no WebP
    }

    @Test
    fun `la respuesta entrega la ruta relativa y la categoria en minusculas`() {
        val respuesta = Datos.archivo(id = "123").aResponse()

        assertEquals("/api/archivos/123", respuesta.ruta)
        assertEquals("evidencia", respuesta.categoria)
        assertEquals("image/jpeg", respuesta.tipoContenido)
    }
}
