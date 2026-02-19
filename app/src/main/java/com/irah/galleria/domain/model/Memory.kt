package com.irah.galleria.domain.model

data class Memory(
    val id: Long,
    val title: String,
    val subtitle: String,
    val cover: Media,
    val items: List<Media>
)
