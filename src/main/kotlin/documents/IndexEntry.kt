package com.vad1mchk.litsearchbot.documents

import kotlinx.serialization.Serializable

@Serializable
data class IndexEntry(
    val original: String,
    val mode: String,
    val extracted: String,
    val hashsum: String,
)
