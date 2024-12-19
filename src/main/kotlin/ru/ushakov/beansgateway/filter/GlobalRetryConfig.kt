package ru.ushakov.beansgateway.filter

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod.*
import org.springframework.http.HttpStatus.*
import org.springframework.retry.annotation.EnableRetry
import java.io.IOException
import java.time.Duration

@Configuration
@EnableRetry
class GlobalRetryConfig : Ordered{

    private val logger: Logger = LoggerFactory.getLogger(GlobalRetryConfig::class.java)

    @Bean
    fun globalRetryFilter(): GlobalFilter {
        val retryConfig = RetryGatewayFilterFactory.RetryConfig()
        retryConfig.retries = 3
        retryConfig.setMethods(
            GET, POST, PUT, PATCH, DELETE
        )
        retryConfig.setStatuses(INTERNAL_SERVER_ERROR,  BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
        retryConfig.setBackoff(Duration.ofMillis(3000), Duration.ofSeconds(10), 2, false)
        retryConfig.setExceptions(
            IOException::class.java
        )

        val retryFilterFactory = RetryGatewayFilterFactory()
        val retryFilter = retryFilterFactory.apply(retryConfig)

        return GlobalFilter { exchange, chain ->
            retryFilter.filter(exchange, chain)
        }
    }

    override fun getOrder(): Int {
        return 0
    }
}