package cmc.mody.common.api.exception

import cmc.mody.common.api.status.ErrorStatus

class GeneralException(
    val status: ErrorStatus
) : RuntimeException(status.message)
