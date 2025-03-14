package com.example.waitr

data class Order(
    var menuItems: MutableList<MenuItem> = mutableListOf(),
    var totalPrice: Double = 0.0,
    var paid: Boolean = false
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "menuItems" to menuItems.map { it.toMap() },
            "totalPrice" to totalPrice,
            "paid" to paid
        )
    }
    fun deleteItem(targetItemId: String): Boolean {
        // Pokud items je null nebo prázdné, není co mazat
        if (menuItems.isEmpty()) return false

        // Odstranění položky s odpovídajícím ID v aktuální skupině
        val removed = menuItems.removeIf { it.id == targetItemId } ?: false
        if (removed) return true

        // Pokud položka nebyla nalezena ani odstraněna
        return false
    }

    fun deepCopy(): Order {
        return Order(
            menuItems = this.menuItems.map { it.deepCopy() }.toMutableList(),
            totalPrice = this.totalPrice,
            paid = this.paid
        )
    }

}
