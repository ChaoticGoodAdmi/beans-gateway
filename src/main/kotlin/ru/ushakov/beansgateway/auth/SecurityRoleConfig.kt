package ru.ushakov.beansgateway.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import ru.ushakov.beansgateway.domain.RouteRule

@Component
@ConfigurationProperties(prefix = "security.roles")
class SecurityRolesConfig {
    var barista: List<RouteRule> = mutableListOf()
    var guest: List<RouteRule> = mutableListOf()
    var common: List<RouteRule> = mutableListOf()
}