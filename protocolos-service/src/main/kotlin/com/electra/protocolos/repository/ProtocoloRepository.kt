package com.electra.protocolos.repository

import com.electra.protocolos.model.Protocolo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** Acceso a la tabla protocolos. Cada filtro es opcional: si llega null, esa condición no se aplica. */
interface ProtocoloRepository : JpaRepository<Protocolo, Long> {

    fun findByNumero(numero: String): Protocolo?

    @Query(
        """
        SELECT p FROM Protocolo p
        WHERE (:creador IS NULL OR p.creadoPor = :creador)
          AND (:desde IS NULL OR p.creadoEn >= :desde)
        ORDER BY p.id DESC
        """
    )
    fun buscar(@Param("creador") creador: String?, @Param("desde") desde: Long?): List<Protocolo>

    /** Mayor id existente (0 si no hay protocolos): sirve para anticipar el próximo número. */
    @Query("SELECT COALESCE(MAX(p.id), 0) FROM Protocolo p")
    fun mayorId(): Long
}
