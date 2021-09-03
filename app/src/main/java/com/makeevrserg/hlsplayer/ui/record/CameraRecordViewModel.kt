package com.makeevrserg.hlsplayer.ui.record

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.*
import com.google.android.exoplayer2.SimpleExoPlayer
import com.makeevrserg.hlsplayer.network.cubicapi.CubicAPI
import com.makeevrserg.hlsplayer.network.cubicapi.response.UserAuthorized
import com.makeevrserg.hlsplayer.network.cubicapi.response.camera.Cameras
import com.makeevrserg.hlsplayer.network.cubicapi.response.camera.timestamp.CameraFileTimestamps
import com.makeevrserg.hlsplayer.network.cubicapi.response.camera.timestamp.CameraFileTimestampsItem
import com.makeevrserg.hlsplayer.network.cubicapi.response.events.Events
import com.makeevrserg.hlsplayer.ui.stream.player.HLSPlayer
import com.makeevrserg.hlsplayer.ui.stream.player.PlayerPositionListener
import com.makeevrserg.hlsplayer.utils.Event
import com.makeevrserg.hlsplayer.utils.Preferences
import com.makeevrserg.hlsplayer.utils.RequestUtils
import com.makeevrserg.hlsplayer.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Принцип работы:
 * Максимальное значение ползунка 24*60*60 - максимальное количество секунд в одном дне.
 *
 * Изначально запускается HLS стрим выбранной камеры.
 *
 * Если пользователь навёл ползунок на время, которое ещё не наступило - начинается HLS стрим. Пользователь уведомлен.
 *
 * При наведении ползунка - получаем два видео. До выбранного времени и после. Считываем время первого файла, после чего считаем разницу в секундах между файлов и выбранным значением. Устанавливаем значение ползнука на время первого видео.
 * Второе видео ставим в плейлист.
 *
 * Для правильного отображения времени используем _updatedTimeline с playerPositionListener. Тут у нас складывается выбранная позиция пользователем(то есть время первого видео) и прогресс текущего видео в плеере.
 *
 * Когда видео доходит до конца - вызывается onMediaItemTransition. Там мы отключаем слушатель позиции плеера и перезаписываем текущее время. После этого снова включаем слушатель и загружаем новое видео. В этот раз берем только второе.
 *
 */
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
        HLSPlayer(
            application.applicationContext,
            "",
            _onMediaItemTransition = { onMediaItemTransition() })


    private val _player = MutableLiveData<SimpleExoPlayer?>(hlsPlayer.player)
    val player: LiveData<SimpleExoPlayer?>
        get() = _player


    /**
     * Таймлайн выбранный пользователем
     */
    private var userSelectedTimeline: Int =
        Utils.getSecondsFromString(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))

    /**
     * При воспроизведении видео из истории - progressBar должен меняться пока идет видео
     */
    private val _updatedTimeline = MutableLiveData<Int>(userSelectedTimeline)
    val updatedTimeLine: LiveData<Int>
        get() = _updatedTimeline


    /**
     * Эвенты которые происходили в течение 24 часов. Их позиции
     */
    private val _events = MutableLiveData<Map<Float, Int>>()
    val events: LiveData<Map<Float, Int>>
        get() = _events

    /**
     * Возвращает позицию ползунка. Для этого нужно сложить выбранную позицию пользователя и текущую позицию плеера
     */
    private val playerPositionListener = PlayerPositionListener(hlsPlayer.player) { position ->
        if (position < 0)
            return@PlayerPositionListener
        _updatedTimeline.value = (position / 1000L).toInt() + userSelectedTimeline
    }

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
        playerPositionListener.disableListener()
        hlsPlayer.setURL(Utils.getHlsUrl(cameras!![cameraIndex].id, getApplication()))
    }


    /**
     * Возвращает реквест видео с текущим временем и датой
     */
    private suspend fun getVideoBySelectedTime(): Any? {
        return RequestUtils.getVideos(
            getCurrentCameraId() ?: return null,
            _date.value ?: return null,
            userSelectedTimeline
        )
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
                val videos = getVideoBySelectedTime()
                if (videos !is CameraFileTimestamps)
                    return@launch
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
            if (response is Utils.ApiResponse) {
                _message.value = Event(RequestUtils.handleResponseMessage(response))
                onLoginRequest()
                stopLoading()
                return@launch
            }
            cameras = response as Cameras

            val list = cameras?.associateBy { it.name }?.keys?.toList() ?: return@launch
            _cameraNames.postValue(list)
            setHlsStream()
            stopLoading()
            getEvents()
        }

    }


    /**
     * Обновление токена пользователя в билдере Retrofit'а
     */
    private fun updateToken(context: Context): Unit =
        CubicAPI.updateToken(Preferences(context).getToken())

    /**
     * Авторизация пользователя
     */
    fun login(login: String, password: String) {

        viewModelScope.launch {
            startLoading()
            val response = Utils.getResponse(CubicAPI.retrofitService.loginUser(login, password))
            if (response is Utils.ApiResponse) {
                _message.postValue(Event(RequestUtils.handleResponseMessage(response)))
                onLoginRequest()
                stopLoading()
                return@launch
            } else if (response is UserAuthorized) {
                Preferences(getApplication()).saveUserAuthorized(response)
                CubicAPI.updateToken(Preferences(getApplication()).getToken())
                getCameras()
            }
            stopLoading()

        }


    }

    /**
     * Добавляет видео в очередь плеера
     */
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
        var toSeek = userSelectedTimeline - fileSeconds
        if (abs(toSeek) > 70) {
            userSelectedTimeline = fileSeconds
            toSeek = 0
            _message.postValue(
                Event(
                    "Ближайшее время ${
                        Utils.getTimeFromSeconds(
                            userSelectedTimeline
                        )
                    }"
                )
            )
        }
        userSelectedTimeline -= toSeek
        viewModelScope.launch(Dispatchers.Main) {
            hlsPlayer.setURL(Utils.getFileUrl(file.file), toSeek * 1000L)
            playerPositionListener.enableListener()
            addPlayerQueue(files.elementAtOrNull(1))
        }
    }

    /**
     * Получение ID текущей выбранной камеры
     */
    private fun getCurrentCameraId() = cameras?.get(cameraIndex)?.id

    /**
     * После выбора камеры ставим Live режим
     */
    fun onCameraSelected(index: Int) {
        cameraIndex = index
        setHlsStream()
        viewModelScope.launch {
            getEvents()
        }

    }

    /**
     * После выбора даты включаем видео
     */
    fun onDateSelected(year: Int, month: Int, day: Int) {
        _date.value = "$year-${month + 1}-$day"
        viewModelScope.launch {
            setVideo()
            getEvents()
        }
    }

    /**
     * Ставит видео с текущей датой и временем
     */
    private suspend fun setVideo() {
        val videos = getVideoBySelectedTime()
        if (videos is Utils.ApiResponse) {
            _message.postValue(Event(RequestUtils.handleResponseMessage(videos)))
            setHlsStream()
        }
        if (videos is CameraFileTimestamps)
            setPlayerHistoryUrl(videos)
    }

    /**
     * Запоминаем значение выбранного таймлайна и включаем ближайшее видео
     */
    fun onProgressChanged(progress: Int?) {
        userSelectedTimeline = progress?:return
        _updatedTimeline.value = userSelectedTimeline
        viewModelScope.launch {
            setVideo()
        }
    }

    /**
     * Получение списка всех эвентов за 24часа текущей камеры и текущего дня
     */
    private suspend fun getEvents() {
        startLoading()
        val request = CubicAPI.retrofitService.getEvents(
            dayFrom = _date.value ?: return,
            camera_ids = arrayListOf(getCurrentCameraId() ?: return)
        )
        val response = Utils.getResponse(request)

        if (response is Utils.ApiResponse) {
            _message.postValue(Event(RequestUtils.handleResponseMessage(response)))
            stopLoading()
            return
        } else if (response is Events) {
            val map = mutableMapOf<Float, Int>()
            for (event in response.data) {
                val pos = Utils.getSecondsFromString(event.created_at.replace(" ", "T"))
                //Здесь можно установить цвета в зависимости от типа эвента
                map[pos.toFloat()] = when (event.type) {
                    "7" -> Color.RED
                    else -> Color.BLACK
                }
            }
            _events.postValue(map)
        }
        stopLoading()

    }

    /**
     * Переключение позиции ползунка на предыдущий эвент.
     *
     * Алгоритм: Берется список доступных эвентов и выбираются только те, которые прошли до текущей позиции.
     *
     * После этого они сортируются по возрастанию. Если прошло больше 10 секунд между последним эвентом и текущим, то выбирается последний.
     * Если прошло меньше, то выбирается предпоследний.
     */
    fun onPrevEventClicked() {
        _updatedTimeline.value?.let { position ->
            val events =
                _events.value?.keys?.filter { it < position }?.sorted()?.toMutableList() ?: return
            if (abs(position - (events.lastOrNull() ?: return)) > 10)
                onProgressChanged(events.lastOrNull()?.toInt() ?: return)
            else {
                events.removeLastOrNull()
                onProgressChanged(events.lastOrNull()?.toInt() ?: return)
            }

        }


    }

    /**
     * Переключение позиции на следующий эвент. Код аналогичен [onPrevEventClicked]. Надо будет нормально переписать
     */
    fun onNextEventClicked() {
        _updatedTimeline.value?.let { position ->
            val events =
                _events.value?.keys?.filter { it > position }?.sorted()?.toMutableList() ?: return
            if (abs(position - (events.firstOrNull() ?: return)) > 10)
                onProgressChanged(events.firstOrNull()?.toInt() ?: return)
            else {
                events.removeFirstOrNull()
                onProgressChanged(events.firstOrNull()?.toInt() ?: return)
            }

        }
    }

    /**
     * Перемотка на 10 секунд вперед
     */
    fun onNext10SecClicked() =
        onProgressChanged(_updatedTimeline.value?.plus(10))


    /**
     * Перемотка на 10 секунд назад
     */
    fun onPrev10SecClicked() =
        onProgressChanged(_updatedTimeline.value?.minus(10))


    init {
        updateToken(application)
        _date.value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        getCameras()
    }
}


