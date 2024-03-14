package com.example.utils

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.asFlux
import reactor.core.publisher.Mono

internal fun monoBridge(block: suspend () -> Unit): Mono<Void> = flow<Unit> { block() }.asFlux().then()

internal fun <T> monoBridge(block: suspend (T) -> Unit): (T) -> Mono<Void> = {
    flow<Unit> { block(it) }.asFlux().then()
}