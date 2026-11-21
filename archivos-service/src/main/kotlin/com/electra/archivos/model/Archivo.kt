package com.electra.archivos.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Tabla "archivos" de la BD electra_archivos. La imagen en sí está en disco ([nombreEnDisco]);
 * en MySQL quedan solo sus datos. El [id] es un UUID aleatorio: nadie puede adivinar la URL
 * de una foto ajena probando números seguidos.
 */
@Entity
@Table(name = "archivos")
class Archivo(
    @Id
    @Column(length = 36)
    var id: String,

    @Column(name = "nombre_original", nullable = false)
    var nombreOriginal: String,

    @Column(name = "tipo_contenido", nullable = false, length = 20)
    var tipoContenido: String,

    @Column(nullable = false)
    var tamano: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var categoria: Categoria,

    /** Login de quien la subió (texto, no llave foránea: cada servicio tiene su propia BD). */
    @Column(name = "subido_por", nullable = false, length = 100)
    var subidoPor: String,

    @Column(name = "subido_en", nullable = false)
    var subidoEn: Long,

    @Column(name = "nombre_en_disco", nullable = false, length = 50)
    var nombreEnDisco: String
)

/** Para qué se usa la imagen: evidencia de una prueba o foto de perfil. */
enum class Categoria { EVIDENCIA, PERFIL }

/** Formatos aceptados, reconocidos por sus primeros bytes (la "firma" del archivo). */
enum class FormatoImagen(val tipoContenido: String, val extension: String) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    companion object {
        /**
         * Se revisa el contenido real y no la extensión ni el Content-Type que manda el cliente,
         * porque ambos se pueden falsificar (p. ej. un .exe renombrado a .jpg).
         */
        fun detectar(bytes: ByteArray): FormatoImagen? = when {
            bytes.empiezaCon(0xFF, 0xD8, 0xFF) -> JPEG
            bytes.empiezaCon(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> PNG
            bytes.size >= 12 && bytes.empiezaCon(0x52, 0x49, 0x46, 0x46) && // "RIFF"
                bytes.copyOfRange(8, 12).contentEquals("WEBP".toByteArray()) -> WEBP
            else -> null
        }

        private fun ByteArray.empiezaCon(vararg firma: Int): Boolean =
            size >= firma.size && firma.indices.all { this[it] == firma[it].toByte() }
    }
}
