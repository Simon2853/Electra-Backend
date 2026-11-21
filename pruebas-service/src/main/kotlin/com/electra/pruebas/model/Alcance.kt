package com.electra.pruebas.model

/**
 * Qué pruebas le corresponden a quien consulta (la misma regla que tenía la app en Alcance.kt):
 * - el técnico solo ve las que creó él;
 * - supervisor y administrador ven las de todos, pero solo desde que se creó su cuenta,
 *   así toda cuenta nueva parte vacía.
 */
data class Alcance(val usuario: String, val veTodo: Boolean, val desde: Long) {

    /** Filtro por creador para la consulta: null = sin filtro. */
    val creador: String? get() = if (veTodo) null else usuario

    /** Filtro por fecha de creación para la consulta: null = sin filtro. */
    val creadosDesde: Long? get() = if (veTodo) desde else null

    fun puedeVer(prueba: Prueba): Boolean =
        if (veTodo) prueba.creadoEn >= desde else prueba.creadoPor == usuario
}
