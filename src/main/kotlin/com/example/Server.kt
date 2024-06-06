package com.example

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import kotlin.time.Duration.Companion.seconds

@SpringBootApplication
class Server

fun main() {
    SpringApplication.run(Server::class.java)
}

private val LOG = LoggerFactory.getLogger(Server::class.java)
private typealias Message = String

@Configuration
@RestController
class WebConfig {
    private val commands = Channel<Message>(RENDEZVOUS)

    @Bean
    fun handlerMapping() = SimpleUrlHandlerMapping(mapOf("/echo" to messageHandler), -1)

    @PostMapping("/commands/stop")
    suspend fun pushCommand() {
        commands.send("STOP")
    }

    @OptIn(DelicateCoroutinesApi::class)
    private val messageHandler = CoroutineWebSocketHandler { session ->
        GlobalScope.launch {
            delay(3.seconds)
            commands.send("some command from upstream")
        }
        val responses = session
            .receive()
            .map { it.payloadAsText }
            .doOnNext { LOG.info("Received: {}", it) }
            .delayElements(1.seconds)
            .map { it.uppercase() }
            .asFlow()

        merge(responses, commands.consumeAsFlow())
            .catch { it.printStackTrace() }
            .let { session.send(it) }
    }
}