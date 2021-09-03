package com.makeevrserg.hlsplayer.network.cubicapi

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.makeevrserg.hlsplayer.network.cubicapi.response.camera.Cameras
import com.makeevrserg.hlsplayer.network.cubicapi.response.UserAuthorized
import com.makeevrserg.hlsplayer.network.cubicapi.response.UserInfo
import com.makeevrserg.hlsplayer.network.cubicapi.response.camera.timestamp.CameraFileTimestamps
import com.makeevrserg.hlsplayer.network.cubicapi.response.events.Events
import com.makeevrserg.hlsplayer.network.cubicapi.response.files.Files
import com.makeevrserg.hlsplayer.utils.Utils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


interface CubicService {
    @POST("auth/login/password")
    public fun loginUser(
        @Query("login") login: String?,
        @Query("password") password: String?
    ): Call<UserAuthorized>


    @GET("files")
    public fun getFiles(): Call<Files>

    @GET("auth/me")
    public fun getUserInfo(): Call<UserInfo>

    @POST("auth/logout")
    public fun logout(
        @Query("refresh_token") refreshToken: String?
    ): Call<Unit>

    @GET("cameras")
    fun getCameras(
        @Query("is_folder") isFolder: Int? = 0
    ): Call<Cameras>

    @GET("files/movies/timestamp/")
    fun getVideoByTimestamp(
        @Query("camera_id") cameraId: Int?,
        @Query("timestamp") timestamp: String?
    ): Call<CameraFileTimestamps>

    @GET("events")
    fun getEvents(
        @Query("from") dayFrom: String = Utils.getCurrentDate(),
        @Query("to") dayTo: String = dayFrom,
        @Query("limit") limit: Int = -1,
        @Query("offset") offset: Int = 0,
        @Query("camera_id[]") camera_ids: ArrayList<Int> = arrayListOf()
    ): Call<Events>

}


object CubicAPI {

    /**
     * Не нашёл нормального способа, как указывать токен пользователя
     */
    private var token: String? = null
    fun updateToken(token: String?) {
        this.token = token
    }

    private fun requestInterceptor() = Interceptor { chain ->
        val url = chain.request()
            .url()
            .newBuilder()
            .build()

        /**
         * Надо как-то привести использование token'а в нормальный вид
         */
        val request = chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer " + token)
            .url(url)
            .build()
        return@Interceptor chain.proceed(request)
    }


    private fun okHttpClient() = OkHttpClient.Builder()
        .addInterceptor(requestInterceptor())
        .build()


    private fun retrofit() = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(okHttpClient())
        .baseUrl("http://213.27.16.25:9000/web-api/")
        .build()

    val retrofitService: CubicService by lazy { retrofit().create(CubicService::class.java) }
}





