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
            exchange.response.statusCode = HttpStatus.FORBIDDEN
            return exchange.response.setComplete()
        }

        val userId = claims[USER_ID_CLAIM].toString()
        val coffeeShopId = claims[COFFEE_SHOP_ID_CLAIM].toString()
        val role = Role.valueOf(claims[ROLE_CLAIM].toString())

        if (!isRoleAuthorized(role, path, method)) {
            logger.info("User role {} is incorrect for this path {} {}", role, method, path)
            exchange.response.statusCode = HttpStatus.FORBIDDEN
            return exchange.response.setComplete()
        }

        logger.info("Mutating request with headers X-UserId = {} and X-CoffeeShopId = {}", userId, coffeeShopId)
        val updatedRequest = exchange.request.mutate()
            .header(USER_ID_HEADER, userId)
            .header(COFFEE_SHOP_ID_HEADER, coffeeShopId)
            .build()

        return chain.filter(exchange.mutate().request(updatedRequest).build())
    }

    private fun isRoleAuthorized(role: Role, path: String, method: String): Boolean {
        val applicableRules = when (role) {
            Role.GUEST -> rolesConfig.guest + rolesConfig.common
            Role.BARISTA -> rolesConfig.barista + rolesConfig.common
        }
        logger.info("User role is {} and rules for this role is {}", role, applicableRules)

        return applicableRules.any { rule ->
            method.equals(rule.method, ignoreCase = true) && pathMatches(rule.path, path)
        }
    }

    private fun pathMatches(pattern: String, path: String): Boolean {
        val regex = pattern.replace("**", ".*").replace("\\{\\w+\\}", "[^/]+").toRegex()
        return regex.matches(path)
    }

/*    private fun getRequiredRoleForPath(path: String, method: String): Role? {
        return when {
            path.startsWith("/coffee-shops") -> {
                if (path.contains("POST") || path.contains("DELETE")) Role.BARISTA
                else null
            }
            path.startsWith("/orders") -> {
                if (path == "/orders" && method == "POST")
                if (path.contains("/coffee-shop") || path.contains("/status")) Role.BARISTA
                else null
            }
            path.startsWith("/loyalty") -> Role.GUEST
            path.startsWith("/insight") -> Role.BARISTA
            path.startsWith("/journal") -> Role.GUEST
            else -> null
        }
    }*/

    override fun getOrder(): Int {
        return Ordered.HIGHEST_PRECEDENCE
    }
}