package com.electra.usuarios.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class Configuracion {

    /** BCrypt agrega una "sal" aleatoria: dos contraseñas iguales producen hashes distintos. */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun openApi(): OpenAPI = OpenAPI().info(
        Info()
            .title("Electra - usuarios-service")
            .version("1.0")
            .description("Cuentas, inicio de sesión, contraseñas, perfil y roles de Electra.")
    )
}
