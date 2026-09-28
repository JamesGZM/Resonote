package com.resonote.core.model

import java.time.LocalDate

data class VipCheckInRecord(val date: LocalDate, val signed: Boolean, val upgraded: Boolean)
data class VipCheckInResult(
    val date: LocalDate,
    val claimed: Boolean,
    val upgraded: Boolean,
    val failure: ContentFailure? = null,
)
