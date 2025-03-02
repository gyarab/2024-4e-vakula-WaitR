package com.example.waitr


data class Settings(
    var companyName: String = "",
    var users: MutableList<User> = mutableListOf()
)
