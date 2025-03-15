package com.example.waitr

data class MenuGroup(
    val id: String = "",
    var name: String = "",
    val items: MutableList<MenuItem> = mutableListOf(),
    val subGroups: MutableList<MenuGroup> = mutableListOf(),
    var locked: String? = null
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "items" to items.map { it.toMap() },
            "subGroups" to subGroups.map { it.toMap() },
            "locked" to locked
        )
    }
    fun deleteGroup(targetId: String): Boolean {
        // Pokud subGroups je null nebo prázdné, není co mazat
        if (subGroups.isEmpty()) return false

        // Odstranění přímé podskupiny s odpovídajícím ID
        val removed = subGroups.removeIf { it.id == targetId }

        // Pokud byla skupina nalezena a odstraněna, vracíme true
        if (removed) return true

        // Rekurzivní kontrola v podskupinách
        subGroups.forEach { subGroup ->
            if (subGroup.deleteGroup(targetId)) return true
        }

        // Pokud nebyla skupina nalezena ani odstraněna
        return false
    }
    fun deleteItem(targetItemId: String): Boolean {
        // Pokud items je null nebo prázdné, není co mazat
        if (items.isNullOrEmpty() && subGroups.isNullOrEmpty()) return false

        // Odstranění položky s odpovídajícím ID v aktuální skupině
        val removed = items?.removeIf { it.id == targetItemId } ?: false
        if (removed) return true

        // Rekurzivní kontrola v podskupinách
        subGroups?.forEach { subGroup ->
            if (subGroup.deleteItem(targetItemId)) return true
        }

        // Pokud položka nebyla nalezena ani odstraněna
        return false
    }

    fun deepCopy(): MenuGroup {
        return MenuGroup(
            id = this.id,
            name = this.name,
            items = this.items.map { it.deepCopy() }.toMutableList(),
            subGroups = this.subGroups.map { it.deepCopy() }.toMutableList()
        )
    }
}