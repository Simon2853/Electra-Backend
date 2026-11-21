package com.electra.archivos.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Dónde se guardan los bytes de las imágenes. Es una interfaz para poder cambiar el disco local
 * por otro almacenamiento (p. ej. en la nube) sin tocar el servicio, y para simularlo en las pruebas.
 */
interface Almacenamiento {
    fun guardar(nombre: String, bytes: ByteArray)
    fun leer(nombre: String): ByteArray?
    fun eliminar(nombre: String)
}

/** Guarda cada imagen como un archivo dentro de la carpeta configurada en archivos.directorio. */
@Component
class AlmacenamientoDisco(@Value("\${archivos.directorio}") directorio: String) : Almacenamiento {

    private val raiz: Path = Paths.get(directorio).toAbsolutePath().normalize()

    init {
        Files.createDirectories(raiz)
    }

    override fun guardar(nombre: String, bytes: ByteArray) {
        Files.write(ruta(nombre), bytes)
    }

    override fun leer(nombre: String): ByteArray? = ruta(nombre).takeIf { Files.exists(it) }?.let(Files::readAllBytes)

    override fun eliminar(nombre: String) {
        Files.deleteIfExists(ruta(nombre))
    }

    /** Impide salir de la carpeta con nombres como "../../secreto" (los nombres los genera el servidor igual). */
    private fun ruta(nombre: String): Path {
        val destino = raiz.resolve(nombre).normalize()
        require(destino.parent == raiz) { "Nombre de archivo inválido: $nombre" }
        return destino
    }
}
