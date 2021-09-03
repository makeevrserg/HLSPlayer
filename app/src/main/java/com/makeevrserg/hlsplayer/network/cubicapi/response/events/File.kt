package com.makeevrserg.hlsplayer.network.cubicapi.response.events

data class File(
    val created_at: String,
    val disk: String,
    val id: Int,
    val path: String,
    val size: Int,
    val updated_at: String
)