package com.example.taskearner

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

