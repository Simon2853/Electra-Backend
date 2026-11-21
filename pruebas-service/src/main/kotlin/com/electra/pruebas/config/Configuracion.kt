package com.electra.pruebas.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Configuracion {

    @Bean
    fun openApi(): OpenAPI = OpenAPI().info(
        Info()
            .title("Electra - pruebas-service")
            .version("1.0")
            .description(
                "Pruebas eléctricas registradas en terreno e historial por equipo. " +
                    "El parámetro usuario indica quién consulta: el técnico ve solo lo suyo; " +
                    "supervisor y administrador ven lo de todos."
            )
    )
}
