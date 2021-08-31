package com.makeevrserg.hlsplayer.network.cubicapi.response

data class UserAuthorized(
    val access_token: String,
    val expires_in: String,
    val refresh_expires_in: String,
    val refresh_token: String,
    val token_type: String
)