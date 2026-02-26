package com.irah.galleria.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Memory(
    val id: Long,
    val title: String,
    val subtitle: String,
    val cover: Media,
    val items: List<Media>
)
