package com.example.waitr

data class MenuItem(
    val id: String,
    var name: String,
    var price: Double,
    var description: String
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