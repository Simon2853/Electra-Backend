package com.electra.archivos.storage

import com.electra.archivos.Datos
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas del guardado en disco, en una carpeta temporal que JUnit crea y borra en cada prueba. */
class AlmacenamientoDiscoTest {

    @TempDir
    lateinit var carpeta: Path

    @Test
    fun `crea la carpeta si no existe`() {
        val nueva = carpeta.resolve("uploads")

        AlmacenamientoDisco(nueva.toString())

        assertTrue(Files.isDirectory(nueva))
    }

    @Test
    fun `guarda, lee y elimina una imagen`() {
        val disco = AlmacenamientoDisco(carpeta.toString())

        disco.guardar("a.jpg", Datos.jpeg)
        assertContentEquals(Datos.jpeg, disco.leer("a.jpg"))

        disco.eliminar("a.jpg")
        assertFalse(Files.exists(carpeta.resolve("a.jpg")))
        assertNull(disco.leer("a.jpg"))
        disco.eliminar("a.jpg") // eliminar algo que ya no está no falla
    }

    @Test
    fun `no permite salir de la carpeta`() {
        val disco = AlmacenamientoDisco(carpeta.resolve("uploads").toString())

        assertThrows<IllegalArgumentException> { disco.guardar("../fuera.jpg", Datos.jpeg) }
        assertThrows<IllegalArgumentException> { disco.leer("sub/carpeta.jpg") }
    }
}
