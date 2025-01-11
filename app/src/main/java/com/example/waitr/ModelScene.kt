package com.example.waitr

data class ModelScene(
    val id: String = "",
    var name: String = "",
    var listOfTables: MutableList<Table> = mutableListOf(),
    var listOfHelpers: MutableList<HelperShape> = mutableListOf()
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "listOfTables" to listOfTables.map { it.toMap() },
            "listOfHelpers" to listOfHelpers.map { it.toMap() }
        )
    }

}
