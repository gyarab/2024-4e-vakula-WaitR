package com.example.waitr
//Objektová třída pro model
data class Model(
    var listOfScenes: MutableList<ModelScene> = mutableListOf(),
    var locked: String? = null
){
    //mapování parametrů pro zápis do databáze
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "listOfScenes" to listOfScenes.map { it.toMap() },
            "locked" to locked
        )
    }
    fun deleteScene(targetSceneId: String): Boolean {
        // Pokud items je null nebo prázdné, není co mazat
        if (listOfScenes.isEmpty()) return false

        // Odstranění položky s odpovídajícím ID v aktuální skupině
        val removed = listOfScenes.removeIf { it.id == targetSceneId } ?: false
        if (removed) return true

        // Pokud položka nebyla nalezena ani odstraněna
        return false
    }

    //kopie dat
    fun deepCopy(): Model {
        return Model(
            listOfScenes = listOfScenes.map { it.deepCopy() }.toMutableList(),
            locked = locked
        )
    }
}
