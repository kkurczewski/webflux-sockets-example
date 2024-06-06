package com.example

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun interface CoroutineWebSocketHandler : WebSocketHandler {
    fun coHandle(session: WebSocketSession): Flow<Any>

    override fun handle(session: WebSocketSession): Mono<Void> = coHandle(session).asFlux().then()
}

fun WebSocketSession.send(messages: Flux<String>): Mono<Void> = this.send(messages.map { this.textMessage(it) })

fun WebSocketSession.send(messages: Flow<String>) = this.send(messages.asFlux()).asFlow()

fun WebSocketSession.messages() = this.receive().asFlow()