package com.vad1mchk.litsearchbot.documents

data class IndexingDeltaStats(
    val runStartedAt: Long,
    val runFinishedAt: Long,
    val upToDate: Int = 0,
    val updated: Int = 0,
    val added: Int = 0,
    val deleted: Int = 0,
    val failed: Int = 0,
)
