package it.ministerodellasalute.verificaC19sdk.model

data class DrlState(
    var dateLastFetch: Long = -1,
    var currentVersion: Long = 0,
    var fromVersion: Long = 0,
    var requestedVersion: Long = 0,
    var totalChunk: Long = 0,
    var chunk: Long = 0,
    var totalNumberUCVI: Long = 0,
    var sizeSingleChunkInByte: Long = 0,
    var currentChunk: Long = 0,
    var deleteNumber: Long = 0,
    var addNumber: Long = 0,
    var totalSizeInByte: Long = 0
)





