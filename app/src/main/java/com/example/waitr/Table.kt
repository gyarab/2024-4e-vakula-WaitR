package com.example.waitr

data class Table(
    val id: String = "",
    var name: String = "",
    var state: String ="",
    var numberOfPeople: Int = 0,
    var listOfCustomers: MutableList<Customer> = mutableListOf(),
    var totalTablePrice: Double = 0.0,
    var height: Int = 0,
    var width: Int = 0,
    var xPosition: Float = 0f,
    var yPosition: Float = 0f,
    var locked: String? = null
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "state" to state,
            "numberOfPeople" to numberOfPeople,
            "listOfCustomers" to listOfCustomers.map { it.toMap() },
            "totalTablePrice" to totalTablePrice,
            "height" to height,
            "width" to width,
            "xPosition" to xPosition,
            "yPosition" to yPosition,
            "locked" to locked
        )
    }

}
