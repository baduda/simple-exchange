package com.example.exchange.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain


@EnableWebFluxSecurity
@Configuration
class SecurityConfiguration {

    @Bean
    @Profile("!test")
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .authorizeExchange { exchanges -> exchanges.anyExchange().authenticated() }
            .oauth2Login { }
            .csrf { it.disable() }
            .build()

    @Bean
    @Profile("test")
    fun securityWebFilterChainTest(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .authorizeExchange { exchanges -> exchanges.anyExchange().authenticated() }
            .csrf { it.disable() }
            .build()
}