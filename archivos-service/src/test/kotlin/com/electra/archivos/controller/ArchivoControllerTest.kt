package com.electra.archivos.controller

import com.electra.archivos.Datos
import com.electra.archivos.exception.ManejadorErrores
import com.electra.archivos.exception.NoEncontrado
import com.electra.archivos.exception.Prohibido
import com.electra.archivos.exception.TipoNoSoportado
import com.electra.archivos.service.ArchivoService
import com.electra.archivos.service.ImagenDescargable
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.multipart.MaxUploadSizeExceededException

/** Pruebas de los endpoints REST de /api/archivos con el servicio simulado (sin servidor, BD ni disco). */
class ArchivoControllerTest {

    private val servicio = mockk<ArchivoService>()
    private val mvc: MockMvc = MockMvcBuilders
        .standaloneSetup(ArchivoController(servicio))
        .setControllerAdvice(ManejadorErrores())
        .build()

    private val foto = MockMultipartFile("archivo", "foto.jpg", "image/jpeg", Datos.jpeg)

    @Test
    fun `POST multipart sube la imagen y responde 201 con su ruta`() {
        every { servicio.subir(any(), "foto.jpg", "evidencia", "camila") } returns Datos.archivo(id = "abc")

        mvc.perform(multipart("/api/archivos").file(foto).param("categoria", "evidencia").param("subidoPor", "camila"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.ruta").value("/api/archivos/abc"))
            .andExpect(jsonPath("$.categoria").value("evidencia"))
    }

    @Test
    fun `POST sin la imagen o sin el usuario responde 400`() {
        mvc.perform(multipart("/api/archivos").param("categoria", "evidencia").param("subidoPor", "camila"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.mensaje").value("Falta la parte archivo con la imagen"))
        mvc.perform(multipart("/api/archivos").file(foto).param("categoria", "evidencia"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.mensaje").value("Falta el parámetro subidoPor"))
    }

    @Test
    fun `POST de algo que no es imagen responde 415 y si es muy grande 413`() {
        every { servicio.subir(any(), any(), any(), "camila") } throws TipoNoSoportado("Solo se aceptan imágenes JPEG, PNG o WebP")
        every { servicio.subir(any(), any(), any(), "ana") } throws MaxUploadSizeExceededException(5_242_880)

        mvc.perform(multipart("/api/archivos").file(foto).param("categoria", "evidencia").param("subidoPor", "camila"))
            .andExpect(status().isUnsupportedMediaType)
        mvc.perform(multipart("/api/archivos").file(foto).param("categoria", "evidencia").param("subidoPor", "ana"))
            .andExpect(status().isPayloadTooLarge)
            .andExpect(jsonPath("$.mensaje").value("La imagen supera el tamaño máximo de 5 MB"))
    }

    @Test
    fun `GET devuelve los bytes con su tipo de contenido y cache`() {
        every { servicio.descargar("abc") } returns ImagenDescargable(Datos.archivo(), Datos.jpeg)

        mvc.perform(get("/api/archivos/abc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("image/jpeg"))
            .andExpect(content().bytes(Datos.jpeg))
            .andExpect(header().string("Cache-Control", "max-age=2592000, public"))
    }

    @Test
    fun `GET de una imagen inexistente responde 404`() {
        every { servicio.descargar("nada") } throws NoEncontrado("No existe el archivo nada")

        mvc.perform(get("/api/archivos/nada")).andExpect(status().isNotFound)
    }

    @Test
    fun `GET info y listado devuelven los datos`() {
        every { servicio.info("abc") } returns Datos.archivo()
        every { servicio.listar("camila@duocuc.cl") } returns listOf(Datos.archivo(), Datos.archivo(id = "def"))

        mvc.perform(get("/api/archivos/abc/info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nombreOriginal").value("foto.jpg"))
        mvc.perform(get("/api/archivos").param("subidoPor", "camila@duocuc.cl"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `DELETE responde 204 o 403 si la imagen es ajena`() {
        justRun { servicio.eliminar("abc", "camila") }
        every { servicio.eliminar("abc", "otro") } throws Prohibido("Solo quien subió la imagen puede eliminarla")

        mvc.perform(delete("/api/archivos/abc").param("usuario", "camila")).andExpect(status().isNoContent)
        mvc.perform(delete("/api/archivos/abc").param("usuario", "otro")).andExpect(status().isForbidden)
    }
}
