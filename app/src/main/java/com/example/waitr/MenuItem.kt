package com.example.waitr

data class MenuItem(
    val id: String = "",
    var name: String = "",
    var price: Double = 0.0,
    var description: String = "",
    var served: Boolean = false
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "price" to price,
            "description" to description
        )
    }
}