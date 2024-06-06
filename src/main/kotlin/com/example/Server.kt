package com.example

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@SpringBootApplication
class Server

fun main() {
    SpringApplication.run(Server::class.java)
}

private val LOG = LoggerFactory.getLogger(Server::class.java)

@Configuration
class WebConfig {
    @Bean
    fun handlerMapping() = SimpleUrlHandlerMapping(mapOf(
        "/echo" to messageHandler
    ), -1)

    private val messageHandler = WebSocketHandler { session ->
        session
            .receive()
            .doOnNext { LOG.info("Received: {}", it.payloadAsText) }
            .map { it.payloadAsText.uppercase() }
            .delayElements(1.seconds.toJavaDuration())
            .map { session.textMessage(it) }
            .doOnError { it.printStackTrace() }
            .let { session.send(it) }
    }
}