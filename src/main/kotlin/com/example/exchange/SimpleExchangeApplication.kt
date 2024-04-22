package com.example.exchange

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SimpleExchangeApplication

fun main(args: Array<String>) {
    runApplication<SimpleExchangeApplication>(*args)
}
