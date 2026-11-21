package com.electra.archivos.repository

import com.electra.archivos.model.Archivo
import org.springframework.data.jpa.repository.JpaRepository

/** Acceso a la tabla archivos (los datos de cada imagen; la imagen está en disco). */
interface ArchivoRepository : JpaRepository<Archivo, String> {
    fun findAllBySubidoPorOrderBySubidoEnDesc(subidoPor: String): List<Archivo>
}
