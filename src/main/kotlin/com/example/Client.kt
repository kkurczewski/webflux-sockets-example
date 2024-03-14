package com.example

import com.example.utils.messages
import com.example.utils.monoBridge
import com.example.utils.sendText
import kotlinx.coroutines.flow.take
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import java.io.IOException
import java.net.URI

private const val ENDPOINT = "ws://localhost:8080/echo"
private val client = ReactorNettyWebSocketClient()

fun main() {
    try {
        client.execute(URI.create(ENDPOINT), messageHandler).block()
    } catch (ex: IOException) {
        System.err.println("Failed due: ${ex.message}")
    } finally {
        println("Socket closed")
    }
}

private val messageHandler = monoBridge<WebSocketSession> { session ->
    session.sendText("Hello")
    session.messages().take(100).collect { incomingMessage ->
        val message = incomingMessage.payloadAsText
        println("Server responded: $message")
        session.sendText("echo")
    }
}