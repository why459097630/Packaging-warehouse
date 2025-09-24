package com.ndjc.app.data.model

data class Comment(val author: String, val content: String)
data class Post(
    val id: String,
    val author: String,
    val content: String,
    val likes: Int = 0,
    val comments: List<Comment> = emptyList()
)
