package com.electra.pruebas

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

/** @EnableFeignClients activa los clientes Feign (ver client/UsuariosClient). */
@EnableFeignClients
@SpringBootApplication
class PruebasServiceApplication

fun main(args: Array<String>) {
	runApplication<PruebasServiceApplication>(*args)
}
