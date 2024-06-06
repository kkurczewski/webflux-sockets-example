package com.example

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.CloseStatus.NORMAL
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URI
import kotlin.time.Duration.Companion.seconds
import com.example.CoroutineWebSocketHandler as handler

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

    val responses = session.messages().mapNotNull { handleMessage(it, job) }
    merge(passiveRequests, responses)
        .catch { it.printStackTrace() }
        .let { session.send(it) }
}

private val messageHandlerJ = WebSocketHandler { session ->
    val job = Job()
    val greet = Mono.just("hello")
    val pings = Flux.just("ping").repeat { job.isActive }.delayElements(1.seconds)
    val bye = session.close(NORMAL).delaySubscription(5.seconds)

    val passiveRequests = greet.concatWith(pings)
    val responses = session.receive().mapNotNullKt { handleMessage(it, job) }

    Flux.merge(passiveRequests, responses)
        .doOnError { it.printStackTrace() }
        .let { session.send(it) }
        .then(bye)
}

private fun handleMessage(message: WebSocketMessage, job: CompletableJob): String? {
    val payload = message.payloadAsText
    LOG.info("Received: {}", payload)

    if (payload == "STOP") {
        job.complete()
        return "OK"
    }
    return null
}