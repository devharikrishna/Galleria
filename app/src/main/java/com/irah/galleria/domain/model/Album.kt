package com.irah.galleria.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Album(
    val id: Long,
    val name: String,
    val relativePath: String?,
    val uri: String, // Cover image URI
    val count: Int,
    val timestamp: Long // Latest timestamp for sorting
)
