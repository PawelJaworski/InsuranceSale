package pl.javorex.insurance.insured

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class InsuredServiceApplication

fun main(args: Array<String>) {
	runApplication<InsuredServiceApplication>(*args)
}
