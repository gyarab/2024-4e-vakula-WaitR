package com.example.waitr

data class Model(
    var listOfScenes: MutableList<ModelScene> = mutableListOf()
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "listOfScenes" to listOfScenes.map { it.toMap() }
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
}
