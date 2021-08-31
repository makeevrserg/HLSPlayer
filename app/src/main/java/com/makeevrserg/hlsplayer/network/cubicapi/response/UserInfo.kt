package com.makeevrserg.hlsplayer.network.cubicapi.response

data class UserInfo(
    val created: String,
    val email: String,
    val id: Int,
    val last_visited: String,
    val login: String,
    val name: String,
    val org_id: Int,
    val photo: String,
    val place_id: Int,
    val role: String,
    val updated_at: String
)