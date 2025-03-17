package com.example.waitr
//Objektová třída pro položku v menu
data class MenuItem(
    val id: String = "",
    var name: String = "",
    var price: Double = 0.0,
    var description: String = "",
    var served: Boolean = false
){
    //mapování parametrů pro zápis do databáze
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "price" to price,
            "description" to description,
            "served" to served

        )
    }
//Kopie dat
    fun deepCopy(): MenuItem {
        return MenuItem(
            id = this.id,
            name = this.name,
            price = this.price,
            description = this.description,
            served = this.served
        )
    }
}