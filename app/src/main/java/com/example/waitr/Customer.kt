package com.example.waitr
//Objektová třída pro zákazníka
data class Customer(
    val id: String = "",
    var name: String = "",
    var order: Order = Order()

){
    //mapování parametrů pro zápis do databáze
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "order" to order.toMap()
        )
    }
//kopie dat
    fun deepCopy(): Customer {
        return Customer(
            id = this.id,
            name = this.name,
            order = this.order.deepCopy()
        )
    }
}
