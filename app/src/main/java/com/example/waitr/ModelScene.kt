package com.example.waitr
//Objektová třída pro scénu v modelu
data class ModelScene(
    val id: String = "",
    var name: String = "",
    var listOfTables: MutableList<Table> = mutableListOf(),
    var listOfHelpers: MutableList<HelperShape> = mutableListOf()
){
    //mapování parametrů pro zápis do databáze
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "listOfTables" to listOfTables.map { it.toMap() },
            "listOfHelpers" to listOfHelpers.map { it.toMap() }
        )
    }
    fun deleteTable(targetTableId: String): Boolean {
        // Pokud items je null nebo prázdné, není co mazat
        if (listOfTables.isEmpty()) return false

        // Odstranění položky s odpovídajícím ID v aktuální skupině
        val removed = listOfTables.removeIf { it.id == targetTableId } ?: false
        if (removed) return true

        // Pokud položka nebyla nalezena ani odstraněna
        return false
    }
    fun deleteHelper(targetTableId: String): Boolean {
        // Pokud items je null nebo prázdné, není co mazat
        if (listOfHelpers.isEmpty()) return false

        // Odstranění položky s odpovídajícím ID v aktuální skupině
        val removed = listOfHelpers.removeIf { it.id == targetTableId } ?: false
        if (removed) return true

        // Pokud položka nebyla nalezena ani odstraněna
        return false
    }

    //kopie dat
    fun deepCopy(): ModelScene {
        return ModelScene(
            id = this.id,
            name = this.name,
            listOfTables = this.listOfTables.map { it.deepCopy() }.toMutableList(),
            listOfHelpers = this.listOfHelpers.map { it.deepCopy() }.toMutableList()
        )
    }

}
