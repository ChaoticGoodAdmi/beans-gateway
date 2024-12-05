package ru.ushakov.beansgateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BeansGatewayApplication

fun main(args: Array<String>) {
	runApplication<BeansGatewayApplication>(*args)
}
