package cmc.mody.common.api

data class ReasonDto(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val httpStatus: Int
)
