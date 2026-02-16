package com.irah.galleria.domain.model

sealed class MediaOperationState {
    object Idle : MediaOperationState()
    data class Running(
        val operation: OperationType,
        val progress: Int,
        val total: Int,
        val currentFile: String
    ) : MediaOperationState()
    data class Completed(val operation: OperationType, val count: Int) : MediaOperationState()
    data class Error(val operation: OperationType, val message: String) : MediaOperationState()
}

enum class OperationType {
    COPY, MOVE, DELETE, RESTORE, DELETE_FOREVER
}
