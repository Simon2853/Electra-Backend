package com.electra.pruebas.service

import com.electra.pruebas.Datos
import com.electra.pruebas.client.ServicioUsuarios
import com.electra.pruebas.exception.NoEncontrado
import com.electra.pruebas.exception.Prohibido
import com.electra.pruebas.exception.SolicitudInvalida
import com.electra.pruebas.model.Alcance
import com.electra.pruebas.model.Prueba
import com.electra.pruebas.repository.PruebaRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.assertEquals

/** Pruebas de las reglas de negocio de PruebaService; repositorio y usuarios-service son mocks. */
class PruebaServiceTest {

    private val repositorio = mockk<PruebaRepository>()
    private val usuarios = mockk<ServicioUsuarios>()
    private lateinit var servicio: PruebaService

    private val alcanceTecnico = Alcance(usuario = Datos.tecnico.usuario, veTodo = false, desde = 100)
    private val alcanceSupervisor = Alcance(usuario = Datos.supervisor.usuario, veTodo = true, desde = 500)

    @BeforeEach
    fun preparar() {
        servicio = PruebaService(repositorio, usuarios, reloj = { 2_000L })
        every { repositorio.save(any()) } answers { firstArg() }
        every { usuarios.obtener(Datos.tecnico.usuario) } returns Datos.tecnico
        every { usuarios.obtener(Datos.supervisor.usuario) } returns Datos.supervisor
        every { usuarios.obtener(Datos.admin.usuario) } returns Datos.admin
        every { usuarios.alcanceDe(Datos.tecnico.usuario) } returns alcanceTecnico
        every { usuarios.alcanceDe(Datos.supervisor.usuario) } returns alcanceSupervisor
        every { usuarios.alcanceDe(Datos.admin.usuario) } returns Alcance(Datos.admin.usuario, true, 0)
    }

    // ---------- registrar ----------

    @Test
    fun `registrar guarda la prueba limpia con el creador y la hora del servidor`() {
        val guardada = slot<Prueba>()
        every { repositorio.save(capture(guardada)) } answers { firstArg() }

        servicio.registrar(Datos.solicitud())

        with(guardada.captured) {
            assertEquals("TR-001", equipo)
            assertEquals("Sin novedad", observaciones)
            assertEquals(Datos.tecnico.usuario, creadoPor)
            assertEquals(2_000L, creadoEn)
            assertEquals(-33.45, latitud)
        }
    }

    @Test
    fun `registrar rechaza un creador que no existe en usuarios-service`() {
        every { usuarios.obtener("nadie") } throws NoEncontrado("No existe el usuario nadie")

        assertThrows<SolicitudInvalida> { servicio.registrar(Datos.solicitud(creadoPor = "nadie")) }
        verify(exactly = 0) { repositorio.save(any()) }
    }

    @Test
    fun `el supervisor no puede registrar pruebas`() {
        assertThrows<Prohibido> { servicio.registrar(Datos.solicitud(creadoPor = Datos.supervisor.usuario)) }
    }

    @Test
    fun `registrar exige latitud y longitud juntas`() {
        assertThrows<SolicitudInvalida> { servicio.registrar(Datos.solicitud(longitud = null)) }
        assertThrows<SolicitudInvalida> { servicio.registrar(Datos.solicitud(latitud = null)) }
        servicio.registrar(Datos.solicitud(latitud = null, longitud = null)) // sin GPS sí se permite
    }

    // ---------- consultas ----------

    @Test
    fun `el tecnico lista solo sus pruebas`() {
        every { repositorio.buscar(Datos.tecnico.usuario, null, null) } returns listOf(Datos.prueba())

        assertEquals(1, servicio.listar(Datos.tecnico.usuario).size)
    }

    @Test
    fun `el supervisor lista las de todos desde la creacion de su cuenta y filtra por equipo`() {
        every { repositorio.buscar(null, 500, "TR-001") } returns listOf(Datos.prueba(), Datos.prueba(id = 2))

        assertEquals(2, servicio.listar(Datos.supervisor.usuario, " TR-001 ").size)
    }

    @Test
    fun `un equipo vacio se trata como sin filtro`() {
        every { repositorio.buscar(Datos.tecnico.usuario, null, null) } returns emptyList()

        servicio.listar(Datos.tecnico.usuario, "  ")

        verify { repositorio.buscar(Datos.tecnico.usuario, null, null) }
    }

    @Test
    fun `buscar devuelve la prueba visible y oculta la ajena como 404`() {
        every { repositorio.findById(1) } returns Optional.of(Datos.prueba(creadoPor = Datos.tecnico.usuario))
        every { repositorio.findById(2) } returns Optional.of(Datos.prueba(id = 2, creadoPor = "otro"))
        every { repositorio.findById(3) } returns Optional.empty()

        assertEquals(1, servicio.buscar(1, Datos.tecnico.usuario).id)
        assertThrows<NoEncontrado> { servicio.buscar(2, Datos.tecnico.usuario) }
        assertThrows<NoEncontrado> { servicio.buscar(3, Datos.tecnico.usuario) }
    }

    @Test
    fun `equiposConRegistros aplica el alcance del usuario`() {
        every { repositorio.equipos(null, 500) } returns listOf("RL-004", "TR-001")

        assertEquals(listOf("RL-004", "TR-001"), servicio.equiposConRegistros(Datos.supervisor.usuario))
    }

    // ---------- eliminar ----------

    @Test
    fun `quien registro la prueba puede eliminarla`() {
        val prueba = Datos.prueba()
        every { repositorio.findById(1) } returns Optional.of(prueba)
        justRun { repositorio.delete(prueba) }

        servicio.eliminar(1, Datos.tecnico.usuario)

        verify { repositorio.delete(prueba) }
    }

    @Test
    fun `el administrador puede eliminar una prueba ajena`() {
        val prueba = Datos.prueba(creadoPor = "otro")
        every { repositorio.findById(1) } returns Optional.of(prueba)
        justRun { repositorio.delete(prueba) }

        servicio.eliminar(1, Datos.admin.usuario)

        verify { repositorio.delete(prueba) }
    }

    @Test
    fun `el supervisor ve pruebas ajenas pero no puede eliminarlas`() {
        every { repositorio.findById(1) } returns Optional.of(Datos.prueba(creadoPor = "otro"))

        assertThrows<Prohibido> { servicio.eliminar(1, Datos.supervisor.usuario) }
        verify(exactly = 0) { repositorio.delete(any()) }
    }
}
