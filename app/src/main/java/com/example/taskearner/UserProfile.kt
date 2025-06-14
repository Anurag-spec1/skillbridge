package com.example.taskearner

data class UserProfile(
    val name: String = "",
    val domain: String = "",
    val email: String = "",
    val github: String = "",
    val linkedin: String = "",
    val instagram: String = "",
    val achievements: String = "",
    val orgname: String = "",
    val profileImage: String = "",
    val access: Boolean = false,
    val skill: String = ""
)