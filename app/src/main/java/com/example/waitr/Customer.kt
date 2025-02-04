package com.example.waitr

data class Customer(
    val id: String = "",
    var name: String = "",
    var order: Order = Order()

){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "order" to order.toMap()
        )
    }
}
