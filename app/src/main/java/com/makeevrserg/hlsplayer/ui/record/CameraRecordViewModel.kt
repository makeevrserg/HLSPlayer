package com.makeevrserg.hlsplayer.ui.record

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.google.android.exoplayer2.SimpleExoPlayer
import com.makeevrserg.hlsplayer.network.cubicapi.CubicAPI
import com.makeevrserg.hlsplayer.network.cubicapi.response.Cameras
import com.makeevrserg.hlsplayer.network.cubicapi.response.camera.timestamp.CameraFileTimestamps
import com.makeevrserg.hlsplayer.network.cubicapi.response.camera.timestamp.CameraFileTimestampsItem
import com.makeevrserg.hlsplayer.ui.stream.player.HLSPlayer
import com.makeevrserg.hlsplayer.ui.stream.player.PlayerPositionListener
import com.makeevrserg.hlsplayer.utils.Event
import com.makeevrserg.hlsplayer.utils.Preferences
import com.makeevrserg.hlsplayer.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.*

class CameraRecordViewModel(application: Application) : AndroidViewModel(application) {


    /**
     * Список полученных камер
     */
    private val _cameraNames = MutableLiveData<List<String>>()
    val cameraNames: LiveData<List<String>>
        get() = _cameraNames

    /**
     * Индикатор загрузки данных
     */
    private val _isLoadong = MutableLiveData<Event<Boolean>>()
    val isLoading: LiveData<Event<Boolean>>
        get() = _isLoadong

    /**
     * Эвент для реквеста авторизации
     */
    private val _authorizationRequest = MutableLiveData<Event<Boolean>>()
    val authorizationRequest: LiveData<Event<Boolean>>
        get() = _authorizationRequest

    /**
     * Выбранная дата
     */
    private val _date = MutableLiveData<String>()
    val date: LiveData<String>
        get() = _date

    /**
     * Уведомления для пользователя
     */
    private val _message = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>>
        get() = _message

    /**
     * Список доступных камер
     */
    private var cameras: Cameras? = null
    private var cameraIndex = 0


    /**
     * Ссылка на HLS Player
     */
    private val hlsPlayer: HLSPlayer =
        HLSPlayer(application.applicationContext, "", _onMediaItemTransition = { onMediaItemTransition() })



    private val _player = MutableLiveData<SimpleExoPlayer?>(hlsPlayer.player)
    val player: LiveData<SimpleExoPlayer?>
        get() = _player


    /**
     * При воспроизведении видео из истории - progressBar должен меняться пока идет видео
     */
    private val _updatedTimeline = MutableLiveData<Int>()
    val updatedTimeLine: LiveData<Int>
        get() = _updatedTimeline

    /**
     * Возвращает позицию ползунка. Для этого нужно сложить выбранную позицию пользователя и текущую позицию плеера
     */
    private val playerPositionListener = PlayerPositionListener(hlsPlayer.player) { position ->
        if (position<0)
            return@PlayerPositionListener
        _updatedTimeline.value = (position / 1000L).toInt() + userSelectedTimeline
    }



    /**
     * Таймлайн выбранный пользователем
     */
    private var userSelectedTimeline: Int = 0
    private val TAG = "CameraRecordViewModel"


    /**
     * Если пользователь не авторизован - создается эвент
     */
    private fun onLoginRequest() {
        _authorizationRequest.postValue(Event(true))
    }

    private fun startLoading() {
        _isLoadong.postValue(Event(true))
    }

    private fun stopLoading() {
        _isLoadong.postValue(Event(false))
    }

    private fun setHlsStream() {
        hlsPlayer.setURL(Utils.getHlsUrl(cameras!![cameraIndex].id, getApplication()))
    }


    /**
     * Вызывается когда видео доходит до конца
     */
    private fun onMediaItemTransition() {
        if (hlsPlayer.isLiveStream())
            return
        playerPositionListener.disableListener()
        _updatedTimeline.value?.let {
            userSelectedTimeline = it
            playerPositionListener.enableListener()
            viewModelScope.launch {
                val videos = getVideos()?.toMutableList() ?: return@launch
                videos.removeAt(0)
                videos.forEach { video ->
                    addPlayerQueue(video)
                }
            }
        }

    }

    /**
     * Получение доступных камер.
     */
    fun getCameras() {
        viewModelScope.launch {
            startLoading()

            val request = CubicAPI.retrofitService.getCameras()
            val response = Utils.getResponse(request)
            if (response is String) {
                viewModelScope.launch(Dispatchers.Main) {
                    _message.value = Event(response)
                    stopLoading()
                }
                onLoginRequest()
                return@launch
            }
            cameras = response as Cameras

            val list = cameras?.associateBy { it.name }?.keys?.toList() ?: return@launch
            _cameraNames.postValue(list)
            setHlsStream()
            stopLoading()
        }

    }


    /**
     * Обновление токена пользователя в билдере Retrofit'а
     */
    private fun updateToken(context: Context): Unit =
        CubicAPI.updateToken(Preferences(context).getToken())

    fun login(login: String, password: String) {
        if (login.isEmpty() || password.isEmpty())
            onLoginRequest()
        viewModelScope.launch {
            startLoading()
            val response = CubicAPI.retrofitService.loginUser(login, password).awaitResponse()

            if (response.message().contains("unauthorized", ignoreCase = true)) {
                onLoginRequest()
                stopLoading()
                return@launch
            }


            if (response.isSuccessful) {
                val user = response.body()
                if (user == null) {
                    Log.d(TAG, "user is null: ${response.message()} ${response.code()}")
                    return@launch
                }
                Log.d(TAG, "Authorization completed: ")
                Preferences(getApplication()).saveUserAuthorized(user)
                CubicAPI.updateToken(Preferences(getApplication()).getToken())
                getCameras()
            } else {
                Log.d(
                    TAG,
                    "response is not Successful: ${response.body()} ${response.message()} ${response.code()}"
                )
            }
            stopLoading()
        }
    }

    private fun addPlayerQueue(file: CameraFileTimestampsItem?) {
        Log.d(TAG, "addPlayerQueue: ${file?.file}")
        hlsPlayer.addUrl(Utils.getFileUrl(file?.file ?: return))
    }

    /**
     * Устанавливаем правильную позицию для плеера и SeekBar'а
     * после чего загружаем первое ближайшее видео и второе, при наличии
     */
    private suspend fun setPlayerHistoryUrl(files: CameraFileTimestamps) {
        val file = files.first()
        val fileSeconds = Utils.getSecondsFromFile(file)
        val toSeek = userSelectedTimeline - fileSeconds
        userSelectedTimeline-=toSeek
        viewModelScope.launch(Dispatchers.Main) {
            hlsPlayer.setURL(Utils.getFileUrl(file.file), toSeek * 1000L)
            playerPositionListener.enableListener()
            addPlayerQueue(files.elementAtOrNull(1))
        }
    }

    private suspend fun getVideos(): CameraFileTimestamps? {
        val cameraId = cameras?.get(cameraIndex)?.id ?: return null
        val timestamp = _date.value + Utils.getTimeFromSecondsT(userSelectedTimeline)
        val request = CubicAPI.retrofitService.getVideoByTimestamp(
            cameraId,
            timestamp
        )
        val response = Utils.getResponse(request)
        if (response is String) {
            viewModelScope.launch(Dispatchers.Main) {
                _message.value = Event(response)
                setHlsStream()
            }
            return null
        }
        return response as CameraFileTimestamps
    }

    /**
     * После выбора камеры ставим Live режим
     */
    fun onCameraSelected(index: Int) {
        cameraIndex = index
        setHlsStream()
    }

    /**
     * После выбора даты включаем видео
     */
    fun onDateSelected(year: Int, month: Int, day: Int) {
        _date.value = "$year-${month + 1}-$day"
        viewModelScope.launch {
            setPlayerHistoryUrl(getVideos() ?: return@launch)
        }
    }

    /**
     * Запоминаем значение выбранного таймлайна и включаем ближайшее видео
     */
    fun onProgressChanged(progress: Int) {
        userSelectedTimeline = progress
        viewModelScope.launch {
            setPlayerHistoryUrl(getVideos() ?: return@launch)
        }
    }

    init {
        updateToken(application)
        _date.value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        getCameras()
    }
}