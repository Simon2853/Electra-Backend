package com.electra.usuarios.repository

import com.electra.usuarios.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository

/** Acceso a la tabla usuarios; Spring Data genera las consultas a partir del nombre de cada método. */
interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByUsuario(usuario: String): Usuario?
    fun existsByUsuario(usuario: String): Boolean
    fun findAllByOrderByNombreAsc(): List<Usuario>
}
