package com.electra.pruebas.controller

import com.electra.pruebas.Datos
import com.electra.pruebas.exception.ManejadorErrores
import com.electra.pruebas.exception.NoEncontrado
import com.electra.pruebas.exception.Prohibido
import com.electra.pruebas.exception.ServicioNoDisponible
import com.electra.pruebas.service.PruebaService
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/** Pruebas de los endpoints REST de /api/pruebas con el servicio simulado (sin servidor ni BD). */
class PruebaControllerTest {

    private val servicio = mockk<PruebaService>()
    private val mvc: MockMvc = MockMvcBuilders
        .standaloneSetup(PruebaController(servicio))
        .setControllerAdvice(ManejadorErrores())
        .build()

    private val cuerpoValido = """
        {"equipo":"TR-001","tipoPrueba":"Resistencia de aislamiento","fecha":"21/11/2026","hora":"14:30",
         "medicion":"125.5","unidad":"MΩ","resultado":"Aprobado","creadoPor":"camila@duocuc.cl"}
    """.trimIndent()

    @Test
    fun `POST registra la prueba y responde 201`() {
        every { servicio.registrar(any()) } returns Datos.prueba(id = 5)

        mvc.perform(post("/api/pruebas").contentType(MediaType.APPLICATION_JSON).content(cuerpoValido))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.creadoPor").value("camila@duocuc.cl"))
    }

    @Test
    fun `POST con formato invalido responde 400 con el error de cada campo`() {
        val cuerpo = """
            {"equipo":"","tipoPrueba":"X","fecha":"2026-11-21","hora":"25:00","medicion":"abc",
             "unidad":"V","resultado":"Quizás","latitud":120,"creadoPor":"camila"}
        """.trimIndent()

        mvc.perform(post("/api/pruebas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campos.equipo").value("El equipo es obligatorio"))
            .andExpect(jsonPath("$.campos.fecha").value("La fecha debe tener el formato dd/MM/aaaa"))
            .andExpect(jsonPath("$.campos.hora").value("La hora debe tener el formato HH:mm"))
            .andExpect(jsonPath("$.campos.medicion").value("La medición debe ser un número (ej: 125.5)"))
            .andExpect(jsonPath("$.campos.resultado").value("El resultado debe ser Aprobado o Rechazado"))
            .andExpect(jsonPath("$.campos.latitud").value("La latitud debe estar entre -90 y 90"))

        verify(exactly = 0) { servicio.registrar(any()) }
    }

    @Test
    fun `POST responde 403 si el rol no puede registrar y 503 si usuarios-service esta caido`() {
        every { servicio.registrar(any()) } throws Prohibido("El rol supervisor no registra pruebas") andThenThrows
            ServicioNoDisponible("usuarios-service no está disponible")

        mvc.perform(post("/api/pruebas").contentType(MediaType.APPLICATION_JSON).content(cuerpoValido))
            .andExpect(status().isForbidden)
        mvc.perform(post("/api/pruebas").contentType(MediaType.APPLICATION_JSON).content(cuerpoValido))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.mensaje").value("usuarios-service no está disponible"))
    }

    @Test
    fun `POST con un cuerpo que no es JSON responde 400`() {
        mvc.perform(post("/api/pruebas").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET lista las pruebas del usuario y filtra por equipo`() {
        every { servicio.listar("camila", "TR-001") } returns listOf(Datos.prueba(), Datos.prueba(id = 2))

        mvc.perform(get("/api/pruebas").param("usuario", "camila").param("equipo", "TR-001"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `GET sin el parametro usuario responde 400`() {
        mvc.perform(get("/api/pruebas"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.mensaje").value("Falta el parámetro usuario"))
    }

    @Test
    fun `GET equipos devuelve los codigos con registros`() {
        every { servicio.equiposConRegistros("camila") } returns listOf("TR-001")

        mvc.perform(get("/api/pruebas/equipos").param("usuario", "camila"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("TR-001"))
    }

    @Test
    fun `GET por id devuelve la prueba o 404`() {
        every { servicio.buscar(1, "camila") } returns Datos.prueba()
        every { servicio.buscar(99, "camila") } throws NoEncontrado("No existe la prueba 99")

        mvc.perform(get("/api/pruebas/1").param("usuario", "camila"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.medicion").value("125.5"))
        mvc.perform(get("/api/pruebas/99").param("usuario", "camila"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE responde 204`() {
        justRun { servicio.eliminar(1, "camila") }

        mvc.perform(delete("/api/pruebas/1").param("usuario", "camila"))
            .andExpect(status().isNoContent)
    }
}
