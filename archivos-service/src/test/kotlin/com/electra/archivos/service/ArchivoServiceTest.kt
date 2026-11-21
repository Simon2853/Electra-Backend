package com.electra.archivos.service

import com.electra.archivos.Datos
import com.electra.archivos.exception.NoEncontrado
import com.electra.archivos.exception.Prohibido
import com.electra.archivos.exception.SolicitudInvalida
import com.electra.archivos.exception.TipoNoSoportado
import com.electra.archivos.model.Archivo
import com.electra.archivos.model.Categoria
import com.electra.archivos.repository.ArchivoRepository
import com.electra.archivos.storage.Almacenamiento
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** Pruebas de las reglas de ArchivoService; repositorio y almacenamiento son mocks. */
class ArchivoServiceTest {

    private val repositorio = mockk<ArchivoRepository>()
    private val almacenamiento = mockk<Almacenamiento>()
    private lateinit var servicio: ArchivoService

    @BeforeEach
    fun preparar() {
        servicio = ArchivoService(repositorio, almacenamiento, reloj = { 7_000L }, nuevoId = { "id-fijo" })
        every { repositorio.save(any()) } answers { firstArg() }
        justRun { almacenamiento.guardar(any(), any()) }
        justRun { almacenamiento.eliminar(any()) }
    }

    // ---------- subir ----------

    @Test
    fun `subir guarda la imagen en disco con la extension de su formato real`() {
        val archivo = servicio.subir(Datos.png, "captura.jpg", "Evidencia", " camila@duocuc.cl ")

        verify { almacenamiento.guardar("id-fijo.png", Datos.png) }
        assertEquals("id-fijo", archivo.id)
        assertEquals("image/png", archivo.tipoContenido) // manda el contenido, no el nombre .jpg
        assertEquals("captura.jpg", archivo.nombreOriginal)
        assertEquals(Categoria.EVIDENCIA, archivo.categoria)
        assertEquals("camila@duocuc.cl", archivo.subidoPor)
        assertEquals(Datos.png.size.toLong(), archivo.tamano)
        assertEquals(7_000L, archivo.subidoEn)
    }

    @Test
    fun `sin nombre original se usa el nombre en disco`() {
        assertEquals("id-fijo.jpg", servicio.subir(Datos.jpeg, null, "perfil", "camila").nombreOriginal)
        assertEquals("id-fijo.webp", servicio.subir(Datos.webp, " ", "perfil", "camila").nombreOriginal)
    }

    @Test
    fun `subir rechaza archivos vacios, categorias desconocidas y usuario vacio`() {
        assertThrows<SolicitudInvalida> { servicio.subir(ByteArray(0), "a.jpg", "evidencia", "camila") }
        assertThrows<SolicitudInvalida> { servicio.subir(Datos.jpeg, "a.jpg", "documento", "camila") }
        assertThrows<SolicitudInvalida> { servicio.subir(Datos.jpeg, "a.jpg", "evidencia", " ") }
        verify(exactly = 0) { almacenamiento.guardar(any(), any()) }
    }

    @Test
    fun `subir rechaza lo que no es imagen aunque se llame foto jpg`() {
        assertThrows<TipoNoSoportado> { servicio.subir(Datos.texto, "foto.jpg", "evidencia", "camila") }
        verify(exactly = 0) { almacenamiento.guardar(any(), any()) }
    }

    @Test
    fun `si la BD falla se borra la imagen del disco para no dejarla huerfana`() {
        every { repositorio.save(any()) } throws IllegalStateException("BD caída")

        assertThrows<IllegalStateException> { servicio.subir(Datos.jpeg, "a.jpg", "evidencia", "camila") }
        verifyOrder {
            almacenamiento.guardar("id-fijo.jpg", Datos.jpeg)
            almacenamiento.eliminar("id-fijo.jpg")
        }
    }

    // ---------- consultar ----------

    @Test
    fun `descargar entrega los datos y los bytes`() {
        every { repositorio.findById("abc") } returns Optional.of(Datos.archivo())
        every { almacenamiento.leer("abc.jpg") } returns Datos.jpeg

        val imagen = servicio.descargar("abc")

        assertEquals("image/jpeg", imagen.archivo.tipoContenido)
        assertContentEquals(Datos.jpeg, imagen.bytes)
    }

    @Test
    fun `descargar responde 404 si no existe el registro o falta la imagen en disco`() {
        every { repositorio.findById("nada") } returns Optional.empty()
        every { repositorio.findById("abc") } returns Optional.of(Datos.archivo())
        every { almacenamiento.leer("abc.jpg") } returns null

        assertThrows<NoEncontrado> { servicio.descargar("nada") }
        assertThrows<NoEncontrado> { servicio.descargar("abc") }
    }

    @Test
    fun `listar busca por quien subio sin espacios`() {
        every { repositorio.findAllBySubidoPorOrderBySubidoEnDesc("camila") } returns listOf(Datos.archivo())

        assertEquals(1, servicio.listar(" camila ").size)
    }

    // ---------- eliminar ----------

    @Test
    fun `quien subio la imagen la elimina de la BD y del disco`() {
        val archivo = Datos.archivo()
        every { repositorio.findById("abc") } returns Optional.of(archivo)
        justRun { repositorio.delete(archivo) }

        servicio.eliminar("abc", "camila@duocuc.cl")

        verify { repositorio.delete(archivo) }
        verify { almacenamiento.eliminar("abc.jpg") }
    }

    @Test
    fun `nadie mas puede eliminar una imagen ajena`() {
        every { repositorio.findById("abc") } returns Optional.of(Datos.archivo())

        assertThrows<Prohibido> { servicio.eliminar("abc", "otro") }
        verify(exactly = 0) { repositorio.delete(any<Archivo>()) }
    }
}
