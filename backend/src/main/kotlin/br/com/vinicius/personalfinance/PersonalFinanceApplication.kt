package br.com.vinicius.personalfinance

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.modulith.Modulithic

@Modulithic(
    systemName = "Personal Finance Platform",
    sharedModules = ["shared"],
)
@SpringBootApplication
class PersonalFinanceApplication

fun main(args: Array<String>) {
    runApplication<PersonalFinanceApplication>(*args)
}
