package com.example

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URI
import kotlin.time.Duration.Companion.seconds

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
        LOG.info("Subscribed")
        emit("hello")
        while (job.isActive) {
            delay(1.seconds)
            emit("ping")
        }
        delay(5.seconds)
        LOG.info("Closing")
    }

    val acks = passiveRequests
        .map { session.textMessage(it) }
        .map { Mono.just(it) }
        .onEach { session.send(it).awaitSingleOrNull() }
        .zip(session.messages()) { _, msg -> handleMessage(msg, job) }
        .filterNotNull()

    session.send(acks).catch { it.printStackTrace() }
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