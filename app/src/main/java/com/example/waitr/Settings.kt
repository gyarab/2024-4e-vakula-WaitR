package com.example.waitr

//objektová struktura pro nastaveni
data class Settings(
    var companyName: String = "",
    var users: MutableList<User> = mutableListOf(),
    var seatedNotSendPeriod: Int = 5,
    var eatingNotSendPeriod: Int = 5,
    var paidNotSendPeriod: Int = 5

)
