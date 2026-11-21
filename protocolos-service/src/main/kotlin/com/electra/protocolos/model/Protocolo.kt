package com.electra.protocolos.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Tabla "protocolos" de la BD electra_protocolos: el documento que agrupa las pruebas de un equipo
 * y que el supervisor formaliza ante el cliente.
 *
 * [numero] ("#000123") se calcula a partir del id que asigna MySQL al insertar: como el id es
 * AUTO_INCREMENT, dos técnicos guardando al mismo tiempo nunca obtienen el mismo número.
 */
@Entity
@Table(name = "protocolos")
class Protocolo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /** Queda null solo un instante, entre el INSERT y la asignación del número en la misma transacción. */
    @Column(unique = true, length = 10)
    var numero: String? = null,

    @Column(nullable = false, length = 20)
    var equipo: String,

    @Column(nullable = false, length = 10)
    var fecha: String,

    @Column(nullable = false, length = 30)
    var estado: String = EstadoProtocolo.EN_TERRENO,

    @Column(nullable = false, length = 100)
    var responsable: String,

    @Column(nullable = false, length = 1000)
    var observaciones: String = "",

    /** Login de usuarios-service (texto, no llave foránea: cada servicio tiene su propia BD). */
    @Column(name = "creado_por", nullable = false, length = 100)
    var creadoPor: String,

    @Column(name = "creado_en", nullable = false)
    var creadoEn: Long,

    /** Quién formalizó y cuándo: queda registrado porque la formalización tiene valor ante el cliente. */
    @Column(name = "formalizado_por", length = 100)
    var formalizadoPor: String? = null,

    @Column(name = "formalizado_en")
    var formalizadoEn: Long? = null
) {
    val formalizado: Boolean get() = estado == EstadoProtocolo.FORMALIZADO
}

/** Estados posibles; los textos son los mismos que muestra la app (EstadosProtocolo). */
object EstadoProtocolo {
    const val EN_TERRENO = "Registrado en terreno"
    const val FORMALIZADO = "Formalizado"
    val TODOS = listOf(EN_TERRENO, FORMALIZADO)
}

/** "#000123" a partir del id; también normaliza lo que llega en la URL ("123", "000123" o "#000123"). */
object NumeroProtocolo {
    fun desdeId(id: Long): String = "#" + id.toString().padStart(6, '0')

    fun normalizar(texto: String): String? =
        texto.trim().removePrefix("#").toLongOrNull()?.takeIf { it > 0 }?.let(::desdeId)
}
