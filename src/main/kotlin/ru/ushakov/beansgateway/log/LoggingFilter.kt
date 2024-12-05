package ru.ushakov.beansgateway.log


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class LoggingFilter : GlobalFilter, Ordered {

    private val log: Logger = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        log.info("Incoming request: {} {} with headers {}", request.method, request.uri, request.headers)

        return chain.filter(exchange).then(Mono.fromRunnable {
            val response: ServerHttpResponse = exchange.response
            log.info("Response for {} {}: status {}", request.method, request.uri, response.statusCode)
        })
    }

    override fun getOrder(): Int {
        return Ordered.LOWEST_PRECEDENCE
    }
}