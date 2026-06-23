package cmc.mody.auth.application.token

import cmc.mody.auth.presentation.dto.TokenDto

interface TokenProvider {
    fun createToken(memberId: Long): TokenDto

    fun validateToken(token: String)

    fun getMemberIdByToken(token: String): Long
}
