package com.irah.galleria.domain.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Immutable

@Stable
sealed class MediaOperationState {
    object Idle : MediaOperationState()
    
    @Immutable
    data class Running(
        val operation: OperationType,
        val progress: Int,
        val total: Int,
        val currentFile: String
    ) : MediaOperationState()

    @Immutable
    data class Completed(val operation: OperationType, val count: Int) : MediaOperationState()

    @Immutable
    data class Error(val operation: OperationType, val message: String) : MediaOperationState()
}

enum class OperationType {
    COPY, MOVE, DELETE, RESTORE, DELETE_FOREVER
}
