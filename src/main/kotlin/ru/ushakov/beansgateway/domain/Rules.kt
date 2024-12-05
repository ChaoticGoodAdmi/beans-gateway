package ru.ushakov.beansgateway.domain

data class RouteRule(
    val method: String,
    val path: String
)

data class RoleRules(
    val barista: List<RouteRule>,
    val guest: List<RouteRule>,
    val common: List<RouteRule>
)