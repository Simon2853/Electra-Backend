package com.electra.protocolos.controller

import com.electra.protocolos.Datos
import com.electra.protocolos.exception.Conflicto
import com.electra.protocolos.exception.ManejadorErrores
import com.electra.protocolos.exception.NoEncontrado
import com.electra.protocolos.exception.Prohibido
import com.electra.protocolos.model.EstadoProtocolo
import com.electra.protocolos.service.ProtocoloService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/** Pruebas de los endpoints REST de /api/protocolos con el servicio simulado (sin servidor ni BD). */
class ProtocoloControllerTest {

    private val servicio = mockk<ProtocoloService>()
    private val mvc: MockMvc = MockMvcBuilders
        .standaloneSetup(ProtocoloController(servicio))
        .setControllerAdvice(ManejadorErrores())
        .build()

    @Test
    fun `POST crea el protocolo y responde 201 con su numero`() {
        every { servicio.crear(any()) } returns Datos.protocolo()

        mvc.perform(
            post("/api/protocolos").contentType(MediaType.APPLICATION_JSON)
                .content("""{"equipo":"TR-001","fecha":"21/11/2026","responsable":"Camila Rojas","creadoPor":"camila@duocuc.cl"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.numero").value("#000123"))
            .andExpect(jsonPath("$.estado").value("Registrado en terreno"))
    }

    @Test
    fun `POST con datos invalidos responde 400 por campo`() {
        mvc.perform(
            post("/api/protocolos").contentType(MediaType.APPLICATION_JSON)
                .content("""{"equipo":"","fecha":"mañana","responsable":"A","creadoPor":""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campos.equipo").value("El equipo es obligatorio"))
            .andExpect(jsonPath("$.campos.fecha").value("La fecha debe tener el formato dd/MM/aaaa"))
            .andExpect(jsonPath("$.campos.responsable").value("El responsable debe tener entre 3 y 100 caracteres"))
            .andExpect(jsonPath("$.campos.creadoPor").value("El creador es obligatorio"))

        verify(exactly = 0) { servicio.crear(any()) }
    }

    @Test
    fun `GET lista los protocolos y el siguiente numero`() {
        every { servicio.listar("camila") } returns listOf(Datos.protocolo())
        every { servicio.siguienteNumero() } returns "#000124"

        mvc.perform(get("/api/protocolos").param("usuario", "camila"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].numero").value("#000123"))
        mvc.perform(get("/api/protocolos/siguiente-numero"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.numero").value("#000124"))
    }

    @Test
    fun `GET de un protocolo acepta el numero codificado y responde 404 si no existe`() {
        every { servicio.buscar("#000123", "camila") } returns Datos.protocolo()
        every { servicio.buscar("999", "camila") } throws NoEncontrado("No existe el protocolo #000999")

        // Como variable de la URL, "#" viaja codificado como %23 (igual que lo hace Retrofit con @Path).
        mvc.perform(get("/api/protocolos/{numero}", "#000123").param("usuario", "camila"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.equipo").value("TR-001"))
        mvc.perform(get("/api/protocolos/999").param("usuario", "camila"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET pruebas del protocolo`() {
        every { servicio.pruebasDelProtocolo("123", "camila") } returns listOf(Datos.prueba)

        mvc.perform(get("/api/protocolos/123/pruebas").param("usuario", "camila"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].unidad").value("MΩ"))
    }

    @Test
    fun `PUT estado formaliza y responde 403 o 409 segun la regla`() {
        val formalizado = Datos.protocolo(estado = EstadoProtocolo.FORMALIZADO)
        every { servicio.cambiarEstado("123", "Formalizado", "jefe") } returns formalizado
        every { servicio.cambiarEstado("123", "Formalizado", "camila") } throws Prohibido("El rol tecnico no puede formalizar protocolos")
        every { servicio.cambiarEstado("8", "Formalizado", "jefe") } throws Conflicto("ya está formalizado")

        mvc.perform(put("/api/protocolos/123/estado").contentType(MediaType.APPLICATION_JSON).content("""{"estado":"Formalizado","usuario":"jefe"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.estado").value("Formalizado"))
        mvc.perform(put("/api/protocolos/123/estado").contentType(MediaType.APPLICATION_JSON).content("""{"estado":"Formalizado","usuario":"camila"}"""))
            .andExpect(status().isForbidden)
        mvc.perform(put("/api/protocolos/8/estado").contentType(MediaType.APPLICATION_JSON).content("""{"estado":"Formalizado","usuario":"jefe"}"""))
            .andExpect(status().isConflict)
    }

    @Test
    fun `PUT estado con un estado inexistente responde 400`() {
        mvc.perform(put("/api/protocolos/123/estado").contentType(MediaType.APPLICATION_JSON).content("""{"estado":"Aprobado","usuario":"jefe"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campos.estado").exists())
    }

    @Test
    fun `PUT observaciones devuelve el protocolo actualizado`() {
        every { servicio.actualizarObservaciones("123", "Nueva", "camila") } returns Datos.protocolo().apply { observaciones = "Nueva" }

        mvc.perform(put("/api/protocolos/123/observaciones").contentType(MediaType.APPLICATION_JSON).content("""{"observaciones":"Nueva","usuario":"camila"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.observaciones").value("Nueva"))
    }

    @Test
    fun `DELETE responde 204 y sin el usuario 400`() {
        justRun { servicio.eliminar("123", "camila") }

        mvc.perform(delete("/api/protocolos/123").param("usuario", "camila"))
            .andExpect(status().isNoContent)
        mvc.perform(delete("/api/protocolos/123"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `un cuerpo que no es JSON responde 400`() {
        mvc.perform(post("/api/protocolos").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest)
    }
}
