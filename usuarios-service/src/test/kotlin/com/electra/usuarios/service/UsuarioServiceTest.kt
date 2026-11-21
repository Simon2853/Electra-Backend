package com.electra.usuarios.service

import com.electra.usuarios.dto.AvatarDto
import com.electra.usuarios.dto.RegistroRequest
import com.electra.usuarios.exception.Conflicto
import com.electra.usuarios.exception.NoAutorizado
import com.electra.usuarios.exception.NoEncontrado
import com.electra.usuarios.exception.SolicitudInvalida
import com.electra.usuarios.model.Rol
import com.electra.usuarios.model.Usuario
import com.electra.usuarios.repository.UsuarioRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pruebas de las reglas de negocio de UsuarioService. El repositorio es un mock (no hay BD);
 * el cifrador es el BCrypt real para comprobar que la contraseña nunca se guarda en texto plano.
 */
class UsuarioServiceTest {

    private val repositorio = mockk<UsuarioRepository>()
    private val cifrador = BCryptPasswordEncoder(4) // costo bajo: misma lógica, pruebas más rápidas
    private lateinit var servicio: UsuarioService

    private fun cuenta(
        usuario: String = "camila@duocuc.cl",
        nombre: String = "Camila Rojas",
        contrasena: String = "123456",
        rol: Rol = Rol.TECNICO
    ) = Usuario(id = 1, usuario = usuario, nombre = nombre, hashContrasena = cifrador.encode(contrasena)!!, rol = rol)

    @BeforeEach
    fun preparar() {
        servicio = UsuarioService(repositorio, cifrador, reloj = { 1_000L })
        every { repositorio.save(any()) } answers { firstArg() }
    }

    // ---------- registrar ----------

    @Test
    fun `registrar guarda la cuenta con la contraseña cifrada y rol tecnico por defecto`() {
        every { repositorio.existsByUsuario("camila@duocuc.cl") } returns false
        val guardado = slot<Usuario>()
        every { repositorio.save(capture(guardado)) } answers { firstArg() }

        servicio.registrar(RegistroRequest(nombre = " Camila Rojas ", usuario = " camila@duocuc.cl ", contrasena = "123456"))

        with(guardado.captured) {
            assertEquals("camila@duocuc.cl", usuario)
            assertEquals("Camila Rojas", nombre)
            assertEquals(Rol.TECNICO, rol)
            assertEquals(1_000L, creadaEn)
            assertNotEquals("123456", hashContrasena)
            assertTrue(cifrador.matches("123456", hashContrasena))
        }
    }

    @Test
    fun `registrar respeta el rol indicado`() {
        every { repositorio.existsByUsuario(any()) } returns false

        val creado = servicio.registrar(RegistroRequest("Ana", "ana", "123456", rol = "supervisor"))

        assertEquals(Rol.SUPERVISOR, creado.rol)
    }

    @Test
    fun `registrar rechaza un usuario repetido`() {
        every { repositorio.existsByUsuario("camila@duocuc.cl") } returns true

        assertThrows<Conflicto> { servicio.registrar(RegistroRequest("Camila", "camila@duocuc.cl", "123456")) }
        verify(exactly = 0) { repositorio.save(any()) }
    }

    @Test
    fun `registrar rechaza un rol desconocido`() {
        every { repositorio.existsByUsuario(any()) } returns false

        assertThrows<SolicitudInvalida> { servicio.registrar(RegistroRequest("Ana", "ana", "123456", rol = "jefe")) }
    }

    // ---------- iniciarSesion ----------

    @Test
    fun `iniciarSesion devuelve la cuenta con credenciales correctas`() {
        every { repositorio.findByUsuario("camila@duocuc.cl") } returns cuenta()

        val resultado = servicio.iniciarSesion(" camila@duocuc.cl ", "123456")

        assertEquals("Camila Rojas", resultado.nombre)
    }

    @Test
    fun `iniciarSesion rechaza una contraseña incorrecta`() {
        every { repositorio.findByUsuario("camila@duocuc.cl") } returns cuenta()

        assertThrows<NoAutorizado> { servicio.iniciarSesion("camila@duocuc.cl", "otra") }
    }

    @Test
    fun `iniciarSesion rechaza un usuario inexistente con el mismo error`() {
        every { repositorio.findByUsuario("nadie") } returns null

        val error = assertThrows<NoAutorizado> { servicio.iniciarSesion("nadie", "123456") }
        assertEquals("Usuario o contraseña incorrectos", error.message)
    }

    // ---------- contraseñas ----------

    @Test
    fun `restablecer cambia la contraseña si el nombre coincide sin importar mayusculas`() {
        val existente = cuenta()
        every { repositorio.findByUsuario("camila@duocuc.cl") } returns existente

        servicio.restablecer("camila@duocuc.cl", "camila rojas", "nueva123")

        assertTrue(cifrador.matches("nueva123", existente.hashContrasena))
    }

    @Test
    fun `restablecer rechaza un nombre que no coincide`() {
        every { repositorio.findByUsuario("camila@duocuc.cl") } returns cuenta()

        assertThrows<SolicitudInvalida> { servicio.restablecer("camila@duocuc.cl", "Otra Persona", "nueva123") }
    }

    @Test
    fun `cambiarContrasena exige la contraseña actual correcta`() {
        val existente = cuenta()
        every { repositorio.findByUsuario("camila@duocuc.cl") } returns existente

        assertThrows<NoAutorizado> { servicio.cambiarContrasena("camila@duocuc.cl", "mala", "nueva123") }
        servicio.cambiarContrasena("camila@duocuc.cl", "123456", "nueva123")

        assertTrue(cifrador.matches("nueva123", existente.hashContrasena))
    }

    // ---------- consultas y perfil ----------

    @Test
    fun `buscar lanza NoEncontrado si el usuario no existe`() {
        every { repositorio.findByUsuario("nadie") } returns null

        assertThrows<NoEncontrado> { servicio.buscar("nadie") }
    }

    @Test
    fun `listar devuelve las cuentas en el orden del repositorio`() {
        every { repositorio.findAllByOrderByNombreAsc() } returns listOf(cuenta(nombre = "Ana"), cuenta(nombre = "Beto"))

        assertEquals(listOf("Ana", "Beto"), servicio.listar().map { it.nombre })
    }

    @Test
    fun `actualizarNombre guarda el nombre sin espacios sobrantes`() {
        every { repositorio.findByUsuario("camila@duocuc.cl") } returns cuenta()

        assertEquals("Camila R.", servicio.actualizarNombre("camila@duocuc.cl", "  Camila R. ").nombre)
    }

    @Test
    fun `actualizarAvatar reemplaza colores y foto`() {
        every { repositorio.findByUsuario("camila@duocuc.cl") } returns cuenta()

        val actualizado = servicio.actualizarAvatar("camila@duocuc.cl", AvatarDto("#FFFFFF", "#0033A0", "http://foto"))

        assertEquals("#FFFFFF", actualizado.avatarColor)
        assertEquals("#0033A0", actualizado.avatarFondo)
        assertEquals("http://foto", actualizado.avatarFoto)
    }

    @Test
    fun `actualizarRol cambia el rol y rechaza claves desconocidas`() {
        every { repositorio.findByUsuario("camila@duocuc.cl") } returns cuenta()

        assertEquals(Rol.ADMINISTRADOR, servicio.actualizarRol("camila@duocuc.cl", "admin").rol)
        assertThrows<SolicitudInvalida> { servicio.actualizarRol("camila@duocuc.cl", "root") }
    }
}
