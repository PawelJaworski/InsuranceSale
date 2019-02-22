package pl.javorex.util.policycreationservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PolicyCreationServiceApplication

fun main(args: Array<String>) {
	runApplication<PolicyCreationServiceApplication>(*args)
}
