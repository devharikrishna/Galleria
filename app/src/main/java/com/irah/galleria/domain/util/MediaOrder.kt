package com.irah.galleria.domain.util
sealed class OrderType {
    object Ascending : OrderType()
    object Descending : OrderType()
}
sealed class MediaOrder(val orderType: OrderType) {
    class Date(orderType: OrderType) : MediaOrder(orderType)
    class Name(orderType: OrderType) : MediaOrder(orderType)
    class Size(orderType: OrderType) : MediaOrder(orderType)

    fun copy(orderType: OrderType): MediaOrder {
        return when(this) {
            is Date -> Date(orderType)
            is Name -> Name(orderType)
            is Size -> Size(orderType)
        }
    }
}