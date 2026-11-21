package com.electra.archivos

import com.electra.archivos.model.Archivo
import com.electra.archivos.model.Categoria

/** Datos de ejemplo: cabeceras mínimas de cada formato (basta con la "firma" de los primeros bytes). */
object Datos {

    val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3)
    val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0)
    val webp = "RIFF".toByteArray() + byteArrayOf(0, 0, 0, 0) + "WEBPVP8 ".toByteArray()
    val texto = "esto no es una imagen".toByteArray()

    fun archivo(id: String = "abc", subidoPor: String = "camila@duocuc.cl") = Archivo(
        id = id, nombreOriginal = "foto.jpg", tipoContenido = "image/jpeg", tamano = jpeg.size.toLong(),
        categoria = Categoria.EVIDENCIA, subidoPor = subidoPor, subidoEn = 1_000, nombreEnDisco = "$id.jpg"
    )
}
