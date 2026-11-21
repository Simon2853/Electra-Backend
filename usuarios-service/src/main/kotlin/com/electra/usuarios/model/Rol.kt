package com.electra.usuarios.model

/**
 * Roles de Electra. [clave] es lo que viaja en el JSON y se guarda en la app (Rol.clave en
 * Kotlin): no cambiarla.
 */
enum class Rol(val clave: String) {
    TECNICO("tecnico"),
    SUPERVISOR("supervisor"),
    ADMINISTRADOR("admin");

    /** Quien formaliza revisa el trabajo de otros, así que ve los registros de todos. */
    val veRegistrosDeTodos: Boolean get() = this != TECNICO

    companion object {
        val POR_DEFECTO = TECNICO

        /** null si la clave no corresponde a ningún rol. */
        fun desdeClave(clave: String?): Rol? = entries.firstOrNull { it.clave == clave }
    }
}
