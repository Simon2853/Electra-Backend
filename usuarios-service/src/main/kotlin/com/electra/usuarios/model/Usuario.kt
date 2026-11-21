package com.electra.usuarios.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/** Tabla "usuarios" de la BD electra_usuarios. La contraseña nunca se guarda: solo su hash BCrypt. */
@Entity
@Table(name = "usuarios")
class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /** Login (p. ej. un correo); único en toda la empresa. */
    @Column(nullable = false, unique = true, length = 100)
    var usuario: String,

    @Column(nullable = false, length = 100)
    var nombre: String,

    @Column(name = "hash_contrasena", nullable = false)
    var hashContrasena: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var rol: Rol = Rol.POR_DEFECTO,

    @Column(name = "avatar_color", length = 20)
    var avatarColor: String? = null,

    @Column(name = "avatar_fondo", length = 20)
    var avatarFondo: String? = null,

    /** URL de la foto de perfil (la entrega archivos-service). */
    @Column(name = "avatar_foto", length = 500)
    var avatarFoto: String? = null,

    /** Creación de la cuenta (epoch ms): la app lo usa para decidir qué registros ve cada rol. */
    @Column(name = "creada_en", nullable = false)
    var creadaEn: Long = System.currentTimeMillis()
)
