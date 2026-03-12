package com.vad1mchk.litsearchbot.documents

data class IndexingTotalStats(
    val totalOnDisk: Int,
    val totalInDatabase: Int,
    val upToDate: Int,
    val outdated: Int,
    val disappeared: Int,
    val unindexed: Int,
    val newestDiskTimestamp: Long,
    val newestIndexedTimestamp: Long,
)
