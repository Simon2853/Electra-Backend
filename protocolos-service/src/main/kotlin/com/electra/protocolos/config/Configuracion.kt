package com.electra.protocolos.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Configuracion {

    @Bean
    fun openApi(): OpenAPI = OpenAPI().info(
        Info()
            .title("Electra - protocolos-service")
            .version("1.0")
            .description(
                "Protocolos de pruebas: número correlativo único, estados y observaciones. " +
                    "El {numero} de la URL puede ir como 123, 000123 o %23000123."
            )
    )
}
