package com.irah.galleria.ui.util

import androidx.compose.runtime.Immutable

@Immutable
data class ImmutableList<T>(val items: List<T>) : List<T> by items

@Immutable
data class ImmutableSet<T>(val items: Set<T>) : Set<T> by items

fun <T> List<T>.toImmutableList(): ImmutableList<T> = ImmutableList(this)
fun <T> Set<T>.toImmutableSet(): ImmutableSet<T> = ImmutableSet(this)
