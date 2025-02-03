package com.example.waitr

data class Order(
    var menuItems: MutableList<MenuItem> = mutableListOf(),
    var totalPrice: Double = 0.0
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "menuItems" to menuItems.map { it.toMap() },
            "totalPrice" to totalPrice
        )
    }

}
