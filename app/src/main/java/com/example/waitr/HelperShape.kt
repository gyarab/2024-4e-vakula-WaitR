package com.example.waitr

data class HelperShape(
    val id: String = "",
    var height: Double = 0.0,
    var width: Double = 0.0,
    var xPosition: Double = 0.0,
    var yPosition: Double = 0.0
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
