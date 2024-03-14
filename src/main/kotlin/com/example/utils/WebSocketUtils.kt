package com.example.utils

import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

internal suspend fun WebSocketSession.sendText(payload: String) {
    this.send(Mono.just(this.textMessage(payload))).awaitSingleOrNull()
}

internal fun WebSocketSession.messages() = this.receive().asFlow()

internal suspend fun WebSocketSession.onEachMessage(
    collector: suspend (WebSocketMessage) -> Unit,
) = this.messages().collect(collector)
