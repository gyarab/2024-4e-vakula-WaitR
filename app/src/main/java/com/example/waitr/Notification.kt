package com.example.waitr

data class Notification(
    var id: String = "",
    var tableId: String = "",
    var type: String = "",
    var timeToSend: Long = 0,
    var send: Boolean = false
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "tableId" to tableId,
            "type" to type,
            "timeToSend" to timeToSend,
            "send" to send
        )
    }
}
