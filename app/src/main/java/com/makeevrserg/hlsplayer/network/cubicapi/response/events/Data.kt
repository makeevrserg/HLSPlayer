package com.makeevrserg.hlsplayer.network.cubicapi.response.events

data class Data(
    val camera_id: Int,
    val camera_name: String,
    val camera_path: List<CameraPath>,
    val confidence: String,
    val created_at: String,
    val `file`: File,
    val file_id: Int,
    val id: Int,
    val info: Info,
    val type: String
)