package com.example.waitr

data class ModelScene(
    val id: String = "",
    var name: String = "",
    var listOfTables: MutableList<Table> = mutableListOf()
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "listOfTables" to listOfTables.map { it.toMap() }
        )
    }

}
