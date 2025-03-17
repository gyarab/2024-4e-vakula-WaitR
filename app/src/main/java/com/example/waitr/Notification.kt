package com.example.waitr
//objektová struktura pro notifikace
data class Notification(
    var id: String = "",
    var tableId: String = "",
    var tableName: String = "",
    var type: String = "",
    var timeToSend: Long = 0,
    var send: Boolean = false
){
    //mapování parametrů pro zápis do databáze
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "tableId" to tableId,
            "tableName" to tableName,
            "type" to type,
            "timeToSend" to timeToSend,
            "send" to send
        )
    }
}
