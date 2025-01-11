package com.example.waitr

data class Model(
    var listOfScenes: MutableList<ModelScene> = mutableListOf()
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "listOfScenes" to listOfScenes.map { it.toMap() }
        )
    }
}
