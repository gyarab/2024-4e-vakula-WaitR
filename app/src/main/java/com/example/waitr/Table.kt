package com.example.waitr

data class Table(
    val id: String = "",
    var name: String = "",
    var state: String ="",
    var numberOfPeople: Int = 0,
    var listOfOrders: MutableList<Order> = mutableListOf(),
    var totalOrderPrice: Double = 0.0,
    var xPosition: Double = 0.0,
    var yPosition: Double = 0.0
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "state" to state,
            "numberOfPeople" to numberOfPeople,
            "listOfOrders" to listOfOrders.map { it.toMap() },
            "totalOrderPrice" to totalOrderPrice,
            "xPosition" to xPosition,
            "yPosition" to yPosition
        )
    }

}
