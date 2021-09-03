package com.makeevrserg.hlsplayer.utils

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import com.makeevrserg.hlsplayer.network.cubicapi.response.camera.timestamp.CameraFileTimestampsItem
import com.makeevrserg.hlsplayer.network.cubicapi.response.files.Movie
import retrofit2.Call
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration

object Utils {
    private val TAG = "Utils"

    /**
     * Возвращает ссылку на стрим
     */
    fun getHlsUrl(cameraId: Int, token: String?) =
        "http://213.27.16.25:9000/video-processor/streams/stream.m3u8?camera=$cameraId&token=$token&type=1"

    /**
     * Возвращает ссылку на стрим
     */
    fun getHlsUrl(cameraId: Int, context: Context) =
        Utils.getHlsUrl(cameraId, Preferences(context).getToken())

    /**
     * Возвращает ссылка на файл
     */
    fun getFileUrl(path: String) =
        "http://213.27.16.25:9000/web-api/storage/$path"


    /**
     * Из заданного файла берет время и конвертирует его в секунды
     */
    fun getSecondsFromFile(file: CameraFileTimestampsItem): Int {
        return getSecondsFromString(file.file)

    }

    /**
     * Из заданного файла берет время и конвертирует его в секунды
     */
    fun getSecondsFromString(path: String): Int {
        val time = path.replace(".mp4", "").split("T").last().split(":")
        val hour = time.elementAtOrNull(0)?.toIntOrNull()
        val minute = time.elementAtOrNull(1)?.toIntOrNull()
        val second = time.elementAtOrNull(2)?.toIntOrNull()
        return getSecondsFromTime(hour, minute, second)

    }


    /**
     * Конвертирует количество секунд, прошедших с момента начала дня в формат понятный человеку
     */
    fun getTimeFromSeconds(progress: Int): String {
        var time = DateUtils.formatElapsedTime(progress * 1L)
        if (time.split(":").first().length < 2)
            time = "0$time"
        return time
    }

    /**
     * Конвертирует часы минуты и секунды в секунды, которые прошли с момента начала дня
     */
    fun getSecondsFromTime(hour: Int?, minute: Int?, seconds: Int?): Int {
        return (seconds ?: 0) + (minute ?: 0) * 60 + (hour ?: 0) * 60 * 60
    }

    /**
     * Конвертирует секунды прошедшие с начала дня в параметр для запроса к API
     */
    fun getTimeFromSecondsT(progress: Int): String {
        return "T${getTimeFromSeconds(progress)}"
    }


    /**
     * Возвращает ответ от API.
     *
     * Надо будет переделать следующимм образом:
     *
     * Возвращаемое значение может быть Any?. Однако будет два типа ApiResponse и T.
     *
     * В случае возвращения ApiResponse не CODE_200 - Что-то пошло не так.
     * @see ApiResponse
     */
    suspend fun <T> getResponse(call: Call<T>): Any {
        val response = call.awaitResponse()
        if (response.code() == 401)
            return ApiResponse.UNAUTHORIZED
        if (response.code() == 404)
            return ApiResponse.CODE_404
        else if (response.code() == 200 && response.body() != null)
            return response.body()!!
        else if (response.code() == 200 && response.body() == null)
            return ApiResponse.CODE_200

        Log.d(TAG, "getResponse: ${response.code()}")
        return ApiResponse.UNEXPECTED_ERROR
    }

    /**
     * Получение текущей даты в формате
     */
    fun getCurrentDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    /**
     * Наиболее частые ответы с сервера
     */
    enum class ApiResponse {
        CODE_200,
        CODE_404,
        UNAUTHORIZED,
        UNEXPECTED_ERROR
    }
}
