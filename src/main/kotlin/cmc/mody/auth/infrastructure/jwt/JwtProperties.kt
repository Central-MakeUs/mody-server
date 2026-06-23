package cmc.mody.auth.infrastructure.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "token")
data class JwtProperties(
    var accessSecret: String = "",
    var accessTokenExpirationTime: Long = 0,
    var refreshTokenExpirationTime: Long = 0
)
