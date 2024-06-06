package com.example

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.CloseStatus.NORMAL
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URI
import kotlin.time.Duration.Companion.seconds
import com.example.CoroutineWebSocketHandler as handler
import kotlin.time.toJavaDuration as java

private const val ENDPOINT = "ws://localhost:9000/echo"
private val LOG = LoggerFactory.getLogger(Client::class.java)

private val client = ReactorNettyWebSocketClient()

class Client

fun main() {
    try {
        client.execute(URI.create(ENDPOINT), messageHandlerJ).block()
    } catch (ex: IOException) {
        LOG.error("Failed due: {}", ex.message)
    } finally {
        LOG.info("Socket closed")
    }
}

private val messageHandler = handler { session ->
    val job = Job()
    val passiveRequests = flow {
        emit("hello")
        while (job.isActive) {
            emit("ping")
            delay(1.seconds)
        }
        delay(5.seconds)
        session.close(NORMAL).awaitSingleOrNull()
    }

    val requests = session.send(passiveRequests)
    val responses = session.messages().onEach {
        val message = it.payloadAsText
        if (message == "STOP") {
            job.complete()
        }
        LOG.info("Received: {}", message)
    }

    merge(requests, responses)
}

private val messageHandlerJ = WebSocketHandler { session ->
    val job = Job()
    val greet = Mono.just("hello")
    val pings = Flux.just("ping").repeat { job.isActive }.delayElements(1.seconds.java())
    val bye = session.close(NORMAL).delaySubscription(5.seconds.java())

    val requests = session.send(greet.concatWith(pings)).then(bye)
    val responses = session.receive().doOnNext {
        val message = it.payloadAsText
        if (message == "STOP") {
            job.complete()
        }
        LOG.info("Received: {}", message)
    }

    Flux.merge(requests, responses).then()
}