package com.makeevrserg.hlsplayer.utils

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.makeevrserg.hlsplayer.network.cubicapi.CubicAPI
import kotlinx.coroutines.launch
import okhttp3.internal.Util
import retrofit2.awaitResponse

object RequestUtils {

    suspend fun getVideos(cameraId: Int?, date: String, seconds: Int): Any {
        val timestamp = date + Utils.getTimeFromSecondsT(seconds)
        val request = CubicAPI.retrofitService.getVideoByTimestamp(
            cameraId,
            timestamp
        )
        return Utils.getResponse(request)
    }


    suspend fun handleResponseMessage(response: Utils.ApiResponse): String {
        return when (response) {
            Utils.ApiResponse.UNAUTHORIZED ->
                "Вы не авторизованы"
            Utils.ApiResponse.CODE_200 -> Utils.ApiResponse.CODE_200.name
            Utils.ApiResponse.UNEXPECTED_ERROR -> "Непредвиденна ошибка"
            Utils.ApiResponse.CODE_404 -> "Ошибка 404"
        }
    }
}