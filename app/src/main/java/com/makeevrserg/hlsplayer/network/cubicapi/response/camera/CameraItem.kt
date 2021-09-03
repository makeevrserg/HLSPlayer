package com.makeevrserg.hlsplayer.network.cubicapi.response.camera

data class CameraItem(
    val compression_percentage: Int,
    val created_at: String,
    val day_record_mode: Int,
    val day_sensitivity: Int,
    val id: Int,
    val is_folder: Boolean,
    val name: String,
    val night_record_mode: Int,
    val night_sensitivity: Int,
    val online: Boolean,
    val order: Int,
    val parent_id: Int,
    val place_id: Int,
    val scripts: List<Int>,
    val snapshot: Snapshot,
    val snapshot_id: Int,
    val time_to_compression: Int,
    val time_to_remove: Int,
    val updated_at: String,
    val url: String
)