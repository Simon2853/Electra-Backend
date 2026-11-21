package com.electra.usuarios.service

import com.electra.usuarios.dto.AvatarDto
import com.electra.usuarios.dto.RegistroRequest
import com.electra.usuarios.exception.Conflicto
import com.electra.usuarios.exception.NoAutorizado
import com.electra.usuarios.exception.NoEncontrado
import com.electra.usuarios.exception.SolicitudInvalida
import com.electra.usuarios.model.Rol
import com.electra.usuarios.model.Usuario
import com.electra.usuarios.repository.UsuarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reglas de negocio de las cuentas. El controlador solo traduce HTTP; todo lo que decide
 * (si un usuario existe, si la contraseña coincide, qué rol es válido) vive aquí.
 * El login se compara sin espacios al inicio ni al final, igual que en la app.
 */
@Service
@Transactional
class UsuarioService(
    private val repositorio: UsuarioRepository,
    private val cifrador: PasswordEncoder,
    private val reloj: () -> Long = System::currentTimeMillis
) {

    fun registrar(datos: RegistroRequest): Usuario {
        val login = datos.usuario.trim()
        if (repositorio.existsByUsuario(login)) throw Conflicto("Ya existe una cuenta con el usuario $login")
        val rol = if (datos.rol.isNullOrBlank()) Rol.POR_DEFECTO else rolValido(datos.rol)
        return repositorio.save(
            Usuario(
                usuario = login,
                nombre = datos.nombre.trim(),
                hashContrasena = cifrador.encode(datos.contrasena)!!,
                rol = rol,
                creadaEn = reloj()
            )
        )
    }

    /** Mismo mensaje si no existe o si la contraseña no coincide: así no se revela qué cuentas existen. */
    @Transactional(readOnly = true)
    fun iniciarSesion(usuario: String, contrasena: String): Usuario {
        val cuenta = repositorio.findByUsuario(usuario.trim())
        if (cuenta == null || !cifrador.matches(contrasena, cuenta.hashContrasena)) {
            throw NoAutorizado("Usuario o contraseña incorrectos")
        }
        return cuenta
    }

    /** Recuperar contraseña: el nombre registrado sirve como comprobación de identidad. */
    fun restablecer(usuario: String, nombre: String, nueva: String) {
        val cuenta = buscar(usuario)
        if (!cuenta.nombre.equals(nombre.trim(), ignoreCase = true)) {
            throw SolicitudInvalida("El nombre no coincide con el de la cuenta")
        }
        cuenta.hashContrasena = cifrador.encode(nueva)!!
    }

    fun cambiarContrasena(usuario: String, actual: String, nueva: String) {
        val cuenta = buscar(usuario)
        if (!cifrador.matches(actual, cuenta.hashContrasena)) throw NoAutorizado("La contraseña actual no es correcta")
        cuenta.hashContrasena = cifrador.encode(nueva)!!
    }

    @Transactional(readOnly = true)
    fun buscar(usuario: String): Usuario =
        repositorio.findByUsuario(usuario.trim()) ?: throw NoEncontrado("No existe el usuario ${usuario.trim()}")

    @Transactional(readOnly = true)
    fun listar(): List<Usuario> = repositorio.findAllByOrderByNombreAsc()

    fun actualizarNombre(usuario: String, nombre: String): Usuario =
        buscar(usuario).apply { this.nombre = nombre.trim() }

    fun actualizarAvatar(usuario: String, avatar: AvatarDto): Usuario =
        buscar(usuario).apply {
            avatarColor = avatar.color
            avatarFondo = avatar.fondo
            avatarFoto = avatar.foto
        }

    fun actualizarRol(usuario: String, clave: String): Usuario {
        val rol = rolValido(clave)
        return buscar(usuario).apply { this.rol = rol }
    }

    private fun rolValido(clave: String): Rol =
        Rol.desdeClave(clave.trim()) ?: throw SolicitudInvalida("Rol desconocido: $clave (use tecnico, supervisor o admin)")
}
