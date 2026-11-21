package com.electra.protocolos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

/** @EnableFeignClients activa los clientes Feign (ver el paquete client). */
@EnableFeignClients
@SpringBootApplication
class ProtocolosServiceApplication

fun main(args: Array<String>) {
	runApplication<ProtocolosServiceApplication>(*args)
}
