package com.electra.eurekaserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer

/**
 * Registro de servicios: cada microservicio se anuncia aquí al arrancar con su nombre
 * (p. ej. "pruebas-service") y el API Gateway le pregunta a Eureka dónde está cada uno.
 * Así nadie depende de IP ni puertos fijos.
 */
@EnableEurekaServer
@SpringBootApplication
class EurekaServerApplication

fun main(args: Array<String>) {
	runApplication<EurekaServerApplication>(*args)
}
