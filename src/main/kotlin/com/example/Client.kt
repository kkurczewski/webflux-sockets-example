package com.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.CloseStatus.NORMAL
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URI
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration as java

private const val ENDPOINT = "ws://localhost:9000/echo"
private val LOG = LoggerFactory.getLogger(Client::class.java)

private val client = ReactorNettyWebSocketClient()

class Client

fun main() {
    try {
        client.execute(URI.create(ENDPOINT), messageHandler).block()
    } catch (ex: IOException) {
        LOG.error("Failed due: {}", ex.message)
    } finally {
        LOG.info("Socket closed")
    }
}

private val messageHandler = WebSocketHandler { session ->
    val passiveRequests = flow {
        emit("hello")
        repeat(5) {
            emit("ping${it + 1}")
            delay(1.seconds)
        }
        delay(5.seconds)
        session.close(NORMAL).awaitSingleOrNull()
    }

    val requests = session.send(passiveRequests)
    val responses = session.messages().onEach {
        LOG.info("Received: {}", it.payloadAsText)
    }

    merge(requests, responses).asFlux().then()
}

private val messageHandlerJ = WebSocketHandler { session ->
    val greet = Mono.just("hello")
    val pings = Flux.interval(1.seconds.java()).map { "ping${it + 1}" }.take(5)
    val bye = session.close(NORMAL).delaySubscription(5.seconds.java())

    val requests = session.send(greet.concatWith(pings)).then(bye)
    val responses = session.receive().doOnNext {
        LOG.info("Received: {}", it.payloadAsText)
    }

    Flux.merge(requests, responses).then()
}

private fun WebSocketSession.send(messages: Flux<String>) = this.send(messages.map { this.textMessage(it) })

private fun WebSocketSession.send(messages: Flow<String>) = this.send(messages.asFlux()).asFlow()

private fun WebSocketSession.messages() = this.receive().asFlow()