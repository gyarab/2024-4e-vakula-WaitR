package com.example.waitr

data class MenuGroup(
    val name: String,
    val items: MutableList<MenuItem>? = mutableListOf(),
    val subGroups: MutableList<MenuGroup>? = mutableListOf()
)