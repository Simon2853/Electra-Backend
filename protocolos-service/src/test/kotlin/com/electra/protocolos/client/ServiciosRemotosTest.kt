package com.electra.protocolos.client

import com.electra.protocolos.Datos
import com.electra.protocolos.exception.NoEncontrado
import com.electra.protocolos.exception.ServicioNoDisponible
import feign.FeignException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pruebas de la comunicación con usuarios-service y pruebas-service (clientes Feign simulados). */
class ServiciosRemotosTest {

    private val usuarios = mockk<UsuariosClient>()
    private val pruebas = mockk<PruebasClient>()
    private val remotos = ServiciosRemotos(usuarios, pruebas)

    private fun falla(estado: Int) = mockk<FeignException> { every { status() } returns estado }

    @Test
    fun `usuario consulta sin espacios y alcanceDe usa rol y creacion de la cuenta`() {
        every { usuarios.obtener("jefe@duocuc.cl") } returns Datos.supervisor

        assertEquals("supervisor", remotos.usuario(" jefe@duocuc.cl ").rol)
        val alcance = remotos.alcanceDe("jefe@duocuc.cl")
        assertTrue(alcance.veTodo)
        assertEquals(0, alcance.desde)
    }

    @Test
    fun `un 404 significa que no existe y otra falla que el servicio no esta disponible`() {
        every { usuarios.obtener("nadie") } throws falla(404)
        every { usuarios.obtener("camila") } throws falla(-1)

        assertThrows<NoEncontrado> { remotos.usuario("nadie") }
        val error = assertThrows<ServicioNoDisponible> { remotos.usuario("camila") }
        assertEquals("usuarios-service no está disponible; intente nuevamente", error.message)
    }

    @Test
    fun `pruebasDe pide a pruebas-service las del equipo y traduce sus fallas`() {
        every { pruebas.listar("camila", "TR-001") } returns listOf(Datos.prueba)
        every { pruebas.listar("camila", "RL-004") } throws falla(503)

        assertEquals(1, remotos.pruebasDe(" camila ", "TR-001").size)
        val error = assertThrows<ServicioNoDisponible> { remotos.pruebasDe("camila", "RL-004") }
        assertEquals("pruebas-service no está disponible; intente nuevamente", error.message)
    }
}
