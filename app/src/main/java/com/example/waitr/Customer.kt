package com.example.waitr

data class Customer(
    val id: String = "",
    var name: String = "",
    var listOfOrders: MutableList<Order> = mutableListOf()

){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "menuItems" to listOfOrders.map { it.toMap() },
        )
    }
}
