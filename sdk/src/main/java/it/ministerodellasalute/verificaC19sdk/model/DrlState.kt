package it.ministerodellasalute.verificaC19sdk.model

data class DrlState(
    var downloadStatus: DownloadStatus? = null,
    var dateLastFetch: Long = 0,
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
) {
}

enum class DownloadStatus {
    DOWNLOAD_AVAILABLE, //Mostra bottone "Scarica ora"
    RESUME_AVAILABLE, //Mostra il bottone "Riprendi"
    COMPLETE, //Download completato, rimuovi view di progress e permetti scan
    DOWNLOADING,//Scaricamento in corso delle revoche
    REQUIRES_CONFIRM,//Richiede conferma per la size del download
}



