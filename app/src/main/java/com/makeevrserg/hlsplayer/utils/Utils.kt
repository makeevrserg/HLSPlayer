package com.makeevrserg.hlsplayer.utils

import android.content.Context
import com.makeevrserg.hlsplayer.network.cubicapi.response.camera.timestamp.CameraFileTimestampsItem
import com.makeevrserg.hlsplayer.network.cubicapi.response.files.Movie
import retrofit2.Call
import retrofit2.awaitResponse

object Utils {
    fun getHlsUrl(cameraId: Int, token: String?) =
        "http://213.27.16.25:9000/video-processor/streams/stream.m3u8?camera=$cameraId&token=$token&type=1"

    fun getHlsUrl(cameraId: Int, context: Context) =
        Utils.getHlsUrl(cameraId, Preferences(context).getToken())

    fun getFileUrl(path: String) =
        "http://213.27.16.25:9000/web-api/storage/$path"


    fun getSecondsFromFile(file: CameraFileTimestampsItem): Int {
        val time = file.file.replace(".mp4", "").split("T").last().split(":")
        val hour = time.elementAtOrNull(0)?.toIntOrNull()
        val minute = time.elementAtOrNull(1)?.toIntOrNull()
        val second = time.elementAtOrNull(2)?.toIntOrNull()
        return getSecondsFromTime(hour, minute, second)

    }

    fun getSecondsFromTime(hour: Int?, minute: Int?, seconds: Int?): Int {
        return (seconds ?: 0) + (minute ?: 0) * 60 + (hour ?: 0) * 60 * 60
    }


    fun getTimeFromSeconds(progress: Int): String {
        val hours = progress / 60 / 60
        val minutes = progress / 60 % 60
        val seconds = progress % 60 % 60
        return "$hours:$minutes:$seconds"
    }

    fun getTimeFromSecondsT(progress: Int): String {
        return "T${getTimeFromSeconds(progress)}"
    }


    suspend fun <T> getResponse(call: Call<T>): Any? {
        val response = call.awaitResponse()
        if (response.code() == 401)
            return "Вы не авторизованы"
        else if (response.code() == 200 && response.body() != null)
            return response.body()

        return "Непредвиденная ошибка"
    }

}
