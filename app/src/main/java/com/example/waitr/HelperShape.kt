package com.example.waitr
//Objektová třída pro pomocný tvar
data class HelperShape(
    val id: String = "",
    var height: Int = 0,
    var width: Int = 0,
    var xPosition: Float = 0f,
    var yPosition: Float = 0f
){
    //mapování parametrů pro zápis do databáze
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "height" to height,
            "width" to width,
            "xPosition" to xPosition,
            "yPosition" to yPosition
        )
    }
    //kopie dat
    fun deepCopy(): HelperShape{
        return HelperShape(
            id = this.id,
            height = this.height,
            width = this.width,
            xPosition = this.xPosition,
            yPosition = this.yPosition
        )
    }

}
