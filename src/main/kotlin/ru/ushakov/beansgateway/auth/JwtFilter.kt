package ru.ushakov.beansgateway.auth

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import ru.ushakov.beansgateway.domain.Role

private const val USER_ID_CLAIM = "userId"
private const val COFFEE_SHOP_ID_CLAIM = "coffeeShopId"
private const val ROLE_CLAIM = "role"

private const val USER_ID_HEADER = "X-UserId"
private const val COFFEE_SHOP_ID_HEADER = "X-CoffeeShopId"

@Component
class JwtFilter(
    private val rolesConfig: SecurityRolesConfig,
    private val jwtUtil: JwtUtil
) : GlobalFilter, Ordered {

    private val logger: Logger = LoggerFactory.getLogger(JwtFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val path = exchange.request.path.toString()
        val method = exchange.request.method.name()
        if (path == "/auth/login" || (path == "/profile" && method == "POST")) {
            return chain.filter(exchange)
        }

        val token = exchange.request.headers.getFirst("Authorization")?.removePrefix("Bearer ")
        if (token.isNullOrEmpty()) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return exchange.response.setComplete()
        }

        val claims = jwtUtil.validateToken(token)
        if (claims == null) {
            logger.warn("Claims are not present in jwtToken")
            exchange.response.statusCode = HttpStatus.FORBIDDEN
            return exchange.response.setComplete()
        }

        val userId = claims[USER_ID_CLAIM].toString()
        val coffeeShopId = claims[COFFEE_SHOP_ID_CLAIM].toString()
        val role = Role.valueOf(claims[ROLE_CLAIM].toString())

        if (!isRoleAuthorized(role, path, method)) {
            logger.warn("User with role {} is not authorized for {} - {}", role, path, method)
            exchange.response.statusCode = HttpStatus.FORBIDDEN
            return exchange.response.setComplete()
        }

        val updatedRequest = exchange.request.mutate()
            .header(USER_ID_HEADER, userId)
            .header(COFFEE_SHOP_ID_HEADER, coffeeShopId)
            .build()

        return chain.filter(exchange.mutate().request(updatedRequest).build())
    }

    private fun isRoleAuthorized(role: Role, path: String, method: String): Boolean {
        if (path.contains("metrics")) return true
        val applicableRules = when (role) {
            Role.GUEST -> rolesConfig.guest + rolesConfig.common
            Role.BARISTA -> rolesConfig.barista + rolesConfig.common
        }

        return applicableRules.any { rule ->
            method.equals(rule.method, ignoreCase = true) && pathMatches(rule.path, path)
        }
    }

    private fun pathMatches(pattern: String, path: String): Boolean {
        val regex = pattern.replace("**", ".*").replace("\\{\\w+\\}", "[^/]+").toRegex()
        return regex.matches(path)
    }

    override fun getOrder(): Int {
        return Ordered.HIGHEST_PRECEDENCE
    }
}