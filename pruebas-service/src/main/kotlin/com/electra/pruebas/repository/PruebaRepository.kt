package com.electra.pruebas.repository

import com.electra.pruebas.model.Prueba
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * Acceso a la tabla pruebas. Cada filtro es opcional: si llega null, esa condición no se aplica
 * (así una sola consulta sirve para el técnico, el supervisor y el historial de un equipo).
 */
interface PruebaRepository : JpaRepository<Prueba, Long> {

    @Query(
        """
        SELECT p FROM Prueba p
        WHERE (:creador IS NULL OR p.creadoPor = :creador)
          AND (:desde IS NULL OR p.creadoEn >= :desde)
          AND (:equipo IS NULL OR p.equipo = :equipo)
        ORDER BY p.id DESC
        """
    )
    fun buscar(
        @Param("creador") creador: String?,
        @Param("desde") desde: Long?,
        @Param("equipo") equipo: String?
    ): List<Prueba>

    @Query(
        """
        SELECT DISTINCT p.equipo FROM Prueba p
        WHERE (:creador IS NULL OR p.creadoPor = :creador)
          AND (:desde IS NULL OR p.creadoEn >= :desde)
        ORDER BY p.equipo ASC
        """
    )
    fun equipos(@Param("creador") creador: String?, @Param("desde") desde: Long?): List<String>
}
