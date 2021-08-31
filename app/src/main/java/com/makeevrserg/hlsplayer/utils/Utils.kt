package com.makeevrserg.hlsplayer.utils

import android.content.Context

object Utils {
    fun getHlsUrl(cameraId: Int, token: String?) =
        "http://213.27.16.25:9000/video-processor/streams/stream.m3u8?camera=$cameraId&token=$token&type=1"

    fun getHlsUrl(cameraId: Int, context: Context) =
        Utils.getHlsUrl(cameraId, Preferences(context).getToken())

    fun getFileUrl(path: String) =
    "http://213.27.16.25:9000/web-api/storage/$path"


}
