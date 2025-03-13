package com.example.waitr

data class HelperShape(
    val id: String = "",
    var height: Int = 0,
    var width: Int = 0,
    var xPosition: Float = 0f,
    var yPosition: Float = 0f
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "height" to height,
            "width" to width,
            "xPosition" to xPosition,
            "yPosition" to yPosition
        )
    }

}
