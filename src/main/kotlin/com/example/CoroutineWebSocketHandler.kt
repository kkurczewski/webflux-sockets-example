package com.example

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession

fun handler(block: (WebSocketSession) -> Flow<*>) = WebSocketHandler { mono { block(it).collect() }.then() }

fun WebSocketSession.send(messages: Flow<String>): Flow<*> = this.send(messages.asFlux().map { this.textMessage(it) }).asFlow()

fun WebSocketSession.messages() = this.receive().asFlow()