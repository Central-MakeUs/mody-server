package cmc.mody.auth.presentation.dto

data class TokenDto(
    val id: Long,
    val accessToken: String,
    val refreshToken: String,
    val isNewMember: Boolean
) {
    companion object {
        fun of(
            id: Long,
            accessToken: String,
            refreshToken: String,
            isNewMember: Boolean = false
        ): TokenDto =
            TokenDto(
                id = id,
                accessToken = accessToken,
                refreshToken = refreshToken,
                isNewMember = isNewMember
            )
    }
}
