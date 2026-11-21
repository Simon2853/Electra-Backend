package com.electra.usuarios.controller

import com.electra.usuarios.exception.Conflicto
import com.electra.usuarios.exception.ManejadorErrores
import com.electra.usuarios.exception.NoAutorizado
import com.electra.usuarios.exception.NoEncontrado
import com.electra.usuarios.model.Rol
import com.electra.usuarios.model.Usuario
import com.electra.usuarios.service.UsuarioService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * Pruebas de los endpoints REST: se simula el servicio y se revisan el código HTTP, el JSON de
 * respuesta y las validaciones. MockMvc ejecuta los controladores sin levantar el servidor ni la BD.
 */
class UsuariosControllersTest {

    private val servicio = mockk<UsuarioService>()
    private val mvc: MockMvc = MockMvcBuilders
        .standaloneSetup(AuthController(servicio), UsuarioController(servicio))
        .setControllerAdvice(ManejadorErrores())
        .build()

    private val camila = Usuario(
        id = 1, usuario = "camila@duocuc.cl", nombre = "Camila Rojas",
        hashContrasena = "hash", rol = Rol.TECNICO, creadaEn = 1_000
    )

    private fun json(texto: String) = texto.trimIndent()

    // ---------- /api/auth ----------

    @Test
    fun `POST registro responde 201 con la cuenta y sin la contraseña`() {
        every { servicio.registrar(any()) } returns camila

        mvc.perform(
            post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(json("""{"nombre":"Camila Rojas","usuario":"camila@duocuc.cl","contrasena":"123456"}"""))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.usuario").value("camila@duocuc.cl"))
            .andExpect(jsonPath("$.rol").value("tecnico"))
            .andExpect(jsonPath("$.hashContrasena").doesNotExist())
    }

    @Test
    fun `POST registro con datos invalidos responde 400 con el error de cada campo`() {
        mvc.perform(
            post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(json("""{"nombre":"","usuario":"ana","contrasena":"123"}"""))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campos.nombre").value("El nombre es obligatorio"))
            .andExpect(jsonPath("$.campos.contrasena").value("La contraseña debe tener entre 6 y 64 caracteres"))

        verify(exactly = 0) { servicio.registrar(any()) }
    }

    @Test
    fun `POST registro de un usuario existente responde 409`() {
        every { servicio.registrar(any()) } throws Conflicto("Ya existe una cuenta con el usuario ana")

        mvc.perform(
            post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(json("""{"nombre":"Ana","usuario":"ana","contrasena":"123456"}"""))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.mensaje").value("Ya existe una cuenta con el usuario ana"))
    }

    @Test
    fun `POST login correcto responde 200 y con credenciales malas 401`() {
        every { servicio.iniciarSesion("camila@duocuc.cl", "123456") } returns camila
        every { servicio.iniciarSesion("camila@duocuc.cl", "mala") } throws NoAutorizado("Usuario o contraseña incorrectos")

        mvc.perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("""{"usuario":"camila@duocuc.cl","contrasena":"123456"}"""))
        ).andExpect(status().isOk).andExpect(jsonPath("$.nombre").value("Camila Rojas"))

        mvc.perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("""{"usuario":"camila@duocuc.cl","contrasena":"mala"}"""))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST con un cuerpo que no es JSON responde 400`() {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("no es json"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud no es un JSON válido"))
    }

    @Test
    fun `POST restablecer responde 204`() {
        justRun { servicio.restablecer("ana", "Ana", "nueva123") }

        mvc.perform(
            post("/api/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
                .content(json("""{"usuario":"ana","nombre":"Ana","nueva":"nueva123"}"""))
        ).andExpect(status().isNoContent)
    }

    // ---------- /api/usuarios ----------

    @Test
    fun `GET usuarios lista las cuentas`() {
        every { servicio.listar() } returns listOf(camila)

        mvc.perform(get("/api/usuarios"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].usuario").value("camila@duocuc.cl"))
    }

    @Test
    fun `GET de un usuario inexistente responde 404`() {
        every { servicio.buscar("nadie") } throws NoEncontrado("No existe el usuario nadie")

        mvc.perform(get("/api/usuarios/nadie"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.estado").value(404))
    }

    @Test
    fun `GET de un usuario existente devuelve sus datos`() {
        every { servicio.buscar("camila@duocuc.cl") } returns camila

        mvc.perform(get("/api/usuarios/camila@duocuc.cl"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.creadaEn").value(1_000))
    }

    @Test
    fun `PUT nombre, avatar y rol devuelven la cuenta actualizada`() {
        every { servicio.actualizarNombre("ana", "Ana María") } returns camila
        every { servicio.actualizarAvatar("ana", any()) } returns camila
        every { servicio.actualizarRol("ana", "admin") } returns camila

        mvc.perform(put("/api/usuarios/ana/nombre").contentType(MediaType.APPLICATION_JSON).content("""{"nombre":"Ana María"}"""))
            .andExpect(status().isOk)
        mvc.perform(put("/api/usuarios/ana/avatar").contentType(MediaType.APPLICATION_JSON).content("""{"color":"#FFF","fondo":"#000"}"""))
            .andExpect(status().isOk)
        mvc.perform(put("/api/usuarios/ana/rol").contentType(MediaType.APPLICATION_JSON).content("""{"rol":"admin"}"""))
            .andExpect(status().isOk)
    }

    @Test
    fun `PUT contrasena responde 204 o 401 segun la contraseña actual`() {
        justRun { servicio.cambiarContrasena("ana", "123456", "nueva123") }
        every { servicio.cambiarContrasena("ana", "mala", "nueva123") } throws NoAutorizado("La contraseña actual no es correcta")

        mvc.perform(put("/api/usuarios/ana/contrasena").contentType(MediaType.APPLICATION_JSON).content("""{"actual":"123456","nueva":"nueva123"}"""))
            .andExpect(status().isNoContent)
        mvc.perform(put("/api/usuarios/ana/contrasena").contentType(MediaType.APPLICATION_JSON).content("""{"actual":"mala","nueva":"nueva123"}"""))
            .andExpect(status().isUnauthorized)
    }
}
