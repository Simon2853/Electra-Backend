package com.electra.protocolos.service

import com.electra.protocolos.Datos
import com.electra.protocolos.client.ServiciosRemotos
import com.electra.protocolos.exception.Conflicto
import com.electra.protocolos.exception.NoEncontrado
import com.electra.protocolos.exception.Prohibido
import com.electra.protocolos.exception.SolicitudInvalida
import com.electra.protocolos.model.Alcance
import com.electra.protocolos.model.EstadoProtocolo
import com.electra.protocolos.model.Protocolo
import com.electra.protocolos.repository.ProtocoloRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/** Pruebas de las reglas de negocio de ProtocoloService; repositorio y servicios remotos son mocks. */
class ProtocoloServiceTest {

    private val repositorio = mockk<ProtocoloRepository>()
    private val remotos = mockk<ServiciosRemotos>()
    private lateinit var servicio: ProtocoloService

    @BeforeEach
    fun preparar() {
        servicio = ProtocoloService(repositorio, remotos, reloj = { 5_000L })
        for (u in listOf(Datos.tecnico, Datos.supervisor, Datos.admin)) {
            every { remotos.usuario(u.usuario) } returns u
            every { remotos.alcanceDe(u.usuario) } returns Alcance(u.usuario, u.veRegistrosDeTodos, u.creadaEn)
        }
        // Simula el AUTO_INCREMENT de MySQL: al guardar, el protocolo recibe el id 124.
        every { repositorio.save(any()) } answers { firstArg<Protocolo>().apply { id = 124 } }
    }

    private fun existe(protocolo: Protocolo) {
        every { repositorio.findByNumero(protocolo.numero!!) } returns protocolo
    }

    // ---------- crear ----------

    @Test
    fun `crear asigna el numero a partir del id y deja el estado en terreno`() {
        val creado = servicio.crear(Datos.solicitud())

        assertEquals("#000124", creado.numero)
        assertEquals(EstadoProtocolo.EN_TERRENO, creado.estado)
        assertEquals("TR-001", creado.equipo)
        assertEquals("Camila Rojas", creado.responsable)
        assertEquals("Revisión anual", creado.observaciones)
        assertEquals(Datos.tecnico.usuario, creado.creadoPor)
        assertEquals(5_000L, creado.creadoEn)
    }

    @Test
    fun `crear rechaza un creador inexistente y al supervisor`() {
        every { remotos.usuario("nadie") } throws NoEncontrado("No existe el usuario nadie")

        assertThrows<SolicitudInvalida> { servicio.crear(Datos.solicitud(creadoPor = "nadie")) }
        assertThrows<Prohibido> { servicio.crear(Datos.solicitud(creadoPor = Datos.supervisor.usuario)) }
        verify(exactly = 0) { repositorio.save(any()) }
    }

    @Test
    fun `siguienteNumero es el mayor id mas uno`() {
        every { repositorio.mayorId() } returns 123

        assertEquals("#000124", servicio.siguienteNumero())
    }

    // ---------- consultas ----------

    @Test
    fun `listar aplica el alcance del usuario`() {
        every { repositorio.buscar(Datos.tecnico.usuario, null) } returns listOf(Datos.protocolo())
        every { repositorio.buscar(null, 0) } returns listOf(Datos.protocolo(), Datos.protocolo(id = 2, creadoPor = "otro"))

        assertEquals(1, servicio.listar(Datos.tecnico.usuario).size)
        assertEquals(2, servicio.listar(Datos.supervisor.usuario).size)
    }

    @Test
    fun `buscar normaliza el numero, oculta los ajenos y rechaza numeros invalidos`() {
        existe(Datos.protocolo())
        existe(Datos.protocolo(id = 7, creadoPor = "otro"))

        assertEquals(123, servicio.buscar("123", Datos.tecnico.usuario).id)
        assertThrows<NoEncontrado> { servicio.buscar("7", Datos.tecnico.usuario) }
        assertThrows<SolicitudInvalida> { servicio.buscar("abc", Datos.tecnico.usuario) }
    }

    @Test
    fun `pruebasDelProtocolo pide a pruebas-service las del equipo del protocolo`() {
        existe(Datos.protocolo())
        every { remotos.pruebasDe(Datos.tecnico.usuario, "TR-001") } returns listOf(Datos.prueba)

        assertEquals(listOf(Datos.prueba), servicio.pruebasDelProtocolo("#000123", Datos.tecnico.usuario))
    }

    // ---------- estados ----------

    @Test
    fun `el supervisor formaliza y queda registrado quien y cuando`() {
        existe(Datos.protocolo())

        val formalizado = servicio.cambiarEstado("123", EstadoProtocolo.FORMALIZADO, Datos.supervisor.usuario)

        assertEquals(EstadoProtocolo.FORMALIZADO, formalizado.estado)
        assertEquals(Datos.supervisor.usuario, formalizado.formalizadoPor)
        assertEquals(5_000L, formalizado.formalizadoEn)
    }

    @Test
    fun `el tecnico no puede formalizar`() {
        existe(Datos.protocolo())

        assertThrows<Prohibido> { servicio.cambiarEstado("123", EstadoProtocolo.FORMALIZADO, Datos.tecnico.usuario) }
    }

    @Test
    fun `un formalizado no vuelve atras y repetir el mismo estado no cambia nada`() {
        val protocolo = Datos.protocolo(estado = EstadoProtocolo.FORMALIZADO)
        existe(protocolo)

        assertEquals(protocolo, servicio.cambiarEstado("123", EstadoProtocolo.FORMALIZADO, Datos.supervisor.usuario))
        assertThrows<Conflicto> { servicio.cambiarEstado("123", EstadoProtocolo.EN_TERRENO, Datos.supervisor.usuario) }
    }

    @Test
    fun `cambiarEstado rechaza estados desconocidos`() {
        assertThrows<SolicitudInvalida> { servicio.cambiarEstado("123", "Aprobado", Datos.supervisor.usuario) }
    }

    // ---------- observaciones y eliminación ----------

    @Test
    fun `las observaciones se editan mientras no este formalizado`() {
        existe(Datos.protocolo())
        existe(Datos.protocolo(id = 8, estado = EstadoProtocolo.FORMALIZADO))

        assertEquals("Cambio de aceite", servicio.actualizarObservaciones("123", "  Cambio de aceite ", Datos.tecnico.usuario).observaciones)
        assertThrows<Conflicto> { servicio.actualizarObservaciones("8", "otra", Datos.tecnico.usuario) }
    }

    @Test
    fun `eliminar lo permite al creador y al admin, nunca si esta formalizado`() {
        val propio = Datos.protocolo()
        val ajeno = Datos.protocolo(id = 9, creadoPor = "otro")
        existe(propio)
        existe(ajeno)
        existe(Datos.protocolo(id = 10, estado = EstadoProtocolo.FORMALIZADO))
        justRun { repositorio.delete(any()) }

        servicio.eliminar("123", Datos.tecnico.usuario)
        servicio.eliminar("9", Datos.admin.usuario)
        assertThrows<Prohibido> { servicio.eliminar("9", Datos.supervisor.usuario) }
        assertThrows<Conflicto> { servicio.eliminar("10", Datos.admin.usuario) }

        verify { repositorio.delete(propio) }
        verify { repositorio.delete(ajeno) }
        verify(exactly = 2) { repositorio.delete(any()) }
    }
}
