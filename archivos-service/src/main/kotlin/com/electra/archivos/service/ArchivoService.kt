package com.electra.archivos.service

import com.electra.archivos.exception.NoEncontrado
import com.electra.archivos.exception.Prohibido
import com.electra.archivos.exception.SolicitudInvalida
import com.electra.archivos.exception.TipoNoSoportado
import com.electra.archivos.model.Archivo
import com.electra.archivos.model.Categoria
import com.electra.archivos.model.FormatoImagen
import com.electra.archivos.repository.ArchivoRepository
import com.electra.archivos.storage.Almacenamiento
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** Imagen lista para enviarse: sus datos y sus bytes. */
class ImagenDescargable(val archivo: Archivo, val bytes: ByteArray)

/**
 * Reglas de los archivos: solo imágenes reales (JPEG, PNG o WebP), cada una con un id imposible
 * de adivinar, y solo quien subió una imagen puede eliminarla.
 */
@Service
@Transactional
class ArchivoService(
    private val repositorio: ArchivoRepository,
    private val almacenamiento: Almacenamiento,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val nuevoId: () -> String = { UUID.randomUUID().toString() }
) {

    fun subir(bytes: ByteArray, nombreOriginal: String?, categoria: String, subidoPor: String): Archivo {
        if (bytes.isEmpty()) throw SolicitudInvalida("La imagen está vacía")
        if (subidoPor.isBlank()) throw SolicitudInvalida("El usuario es obligatorio")
        val tipo = Categoria.entries.firstOrNull { it.name.equals(categoria.trim(), ignoreCase = true) }
            ?: throw SolicitudInvalida("Categoría desconocida: $categoria (use evidencia o perfil)")
        val formato = FormatoImagen.detectar(bytes)
            ?: throw TipoNoSoportado("Solo se aceptan imágenes JPEG, PNG o WebP")

        val id = nuevoId()
        val nombreEnDisco = "$id.${formato.extension}"
        almacenamiento.guardar(nombreEnDisco, bytes)
        return try {
            repositorio.save(
                Archivo(
                    id = id,
                    nombreOriginal = nombreOriginal?.takeIf { it.isNotBlank() }?.take(255) ?: nombreEnDisco,
                    tipoContenido = formato.tipoContenido,
                    tamano = bytes.size.toLong(),
                    categoria = tipo,
                    subidoPor = subidoPor.trim(),
                    subidoEn = reloj(),
                    nombreEnDisco = nombreEnDisco
                )
            )
        } catch (e: RuntimeException) {
            almacenamiento.eliminar(nombreEnDisco) // sin datos en la BD, la imagen en disco quedaría huérfana
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun info(id: String): Archivo = repositorio.findById(id).orElse(null) ?: throw NoEncontrado("No existe el archivo $id")

    @Transactional(readOnly = true)
    fun descargar(id: String): ImagenDescargable {
        val archivo = info(id)
        val bytes = almacenamiento.leer(archivo.nombreEnDisco) ?: throw NoEncontrado("La imagen $id ya no está disponible")
        return ImagenDescargable(archivo, bytes)
    }

    @Transactional(readOnly = true)
    fun listar(subidoPor: String): List<Archivo> = repositorio.findAllBySubidoPorOrderBySubidoEnDesc(subidoPor.trim())

    fun eliminar(id: String, usuario: String) {
        val archivo = info(id)
        if (archivo.subidoPor != usuario.trim()) throw Prohibido("Solo quien subió la imagen puede eliminarla")
        repositorio.delete(archivo)
        almacenamiento.eliminar(archivo.nombreEnDisco)
    }
}
