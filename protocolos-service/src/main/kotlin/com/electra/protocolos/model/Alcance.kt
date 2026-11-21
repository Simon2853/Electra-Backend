package com.electra.protocolos.model

/**
 * Qué protocolos le corresponden a quien consulta (la misma regla que pruebas-service):
 * - el técnico solo ve los que creó él;
 * - supervisor y administrador ven los de todos, desde que se creó su cuenta.
 */
data class Alcance(val usuario: String, val veTodo: Boolean, val desde: Long) {

    /** Filtro por creador para la consulta: null = sin filtro. */
    val creador: String? get() = if (veTodo) null else usuario

    /** Filtro por fecha de creación para la consulta: null = sin filtro. */
    val creadosDesde: Long? get() = if (veTodo) desde else null

    fun puedeVer(protocolo: Protocolo): Boolean =
        if (veTodo) protocolo.creadoEn >= desde else protocolo.creadoPor == usuario
}
