package com.example.waitr

data class Order(
    val id: String = "",
    var name: String = "",
    var menuItems: MutableList<MenuItem> = mutableListOf(),
    var totalPrice: Double = 0.0
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "menuItems" to menuItems.map { it.toMap() },
            "totalPrice" to totalPrice
        )
    }

}
