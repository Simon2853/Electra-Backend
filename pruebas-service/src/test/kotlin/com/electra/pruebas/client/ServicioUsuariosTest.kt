package com.electra.pruebas.client

import com.electra.pruebas.Datos
import com.electra.pruebas.exception.NoEncontrado
import com.electra.pruebas.exception.ServicioNoDisponible
import feign.FeignException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pruebas de la comunicación con usuarios-service: el cliente Feign se simula con MockK. */
class ServicioUsuariosTest {

    private val cliente = mockk<UsuariosClient>()
    private val servicio = ServicioUsuarios(cliente)

    private fun falla(estado: Int) = mockk<FeignException> { every { status() } returns estado }

    @Test
    fun `obtener consulta el usuario sin espacios sobrantes`() {
        every { cliente.obtener("camila@duocuc.cl") } returns Datos.tecnico

        assertEquals("tecnico", servicio.obtener("  camila@duocuc.cl ").rol)
    }

    @Test
    fun `un 404 de usuarios-service significa que el usuario no existe`() {
        every { cliente.obtener("nadie") } throws falla(404)

        assertThrows<NoEncontrado> { servicio.obtener("nadie") }
    }

    @Test
    fun `cualquier otra falla de usuarios-service responde servicio no disponible`() {
        every { cliente.obtener("camila") } throws falla(500)
        every { cliente.obtener("ana") } throws falla(-1) // sin conexión: Feign no recibe código HTTP

        assertThrows<ServicioNoDisponible> { servicio.obtener("camila") }
        assertThrows<ServicioNoDisponible> { servicio.obtener("ana") }
    }

    @Test
    fun `alcanceDe arma el alcance segun el rol y la creacion de la cuenta`() {
        every { cliente.obtener("jefe@duocuc.cl") } returns Datos.supervisor

        val alcance = servicio.alcanceDe("jefe@duocuc.cl")

        assertTrue(alcance.veTodo)
        assertEquals(500, alcance.desde)
        assertEquals("jefe@duocuc.cl", alcance.usuario)
    }
}
