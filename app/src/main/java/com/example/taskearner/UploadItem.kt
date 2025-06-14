package com.example.taskearner

data class UploadItem(
    var uploadId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val domain: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val link: String = "",
    val timestamp: Long = 0L,
    var likes: Int = 0,
    var numcomment: Int = 0,
    val github: String = "",
    val linkedin: String = "",
    val instagram: String = "",
    val skill: String = "",
    val achievements: String = "",
    val orgname: String = "",
    var likedBy: Map<String, Boolean> = emptyMap()
)
