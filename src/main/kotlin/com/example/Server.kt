package com.example

import com.example.utils.monoBridge
import com.example.utils.onEachMessage
import com.example.utils.sendText
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession

@SpringBootApplication
class Server

fun main() {
    SpringApplication.run(Server::class.java)
}

@Configuration
class WebConfig {
    @Bean
    fun handlerMapping() = SimpleUrlHandlerMapping(mapOf(
        "/echo" to EchoHandler()
    ), -1)
}

class EchoHandler : WebSocketHandler {
    override fun handle(session: WebSocketSession) = monoBridge {
        session.onEachMessage {
            val payloadAsText = it.payloadAsText
            println("Client sent: $payloadAsText")
            session.sendText(payloadAsText.uppercase())
        }
    }
}