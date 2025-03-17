package com.example.waitr
//pomocná datová struktura pro lepší ukládaní dat pro uživatele z analytics do listu
data class Users_data(
    val id: String,
    val numberOfServedTables: Int,
    val activity: Int
)
