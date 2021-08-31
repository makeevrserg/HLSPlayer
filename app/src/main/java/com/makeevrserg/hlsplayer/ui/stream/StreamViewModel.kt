package com.makeevrserg.hlsplayer.ui.stream

import android.app.Application
import androidx.lifecycle.*
import com.google.android.exoplayer2.SimpleExoPlayer
import com.makeevrserg.hlsplayer.ui.stream.player.ConnectionManager
import com.makeevrserg.hlsplayer.ui.stream.player.HLSPlayer


class StreamViewModel(application: Application) : AndroidViewModel(application) {


    /**
     * Эта секция только для проверки работоспособности
     *
     * map с названиями стримов(для отображения пользоветлю) и их ссылками
     */
    val STREAMING_URLS = mapOf(
        "Интервью" to "https://moctobpltc-i.akamaihd.net/hls/live/571329/eight/playlist.m3u8",
        "Вход в офис(глючит(Не проблема плеера))" to "http://213.27.16.25:9000/video-processor/streams/stream.m3u8?camera=3&token=1&type=1",
        "Офис(без звука)" to "http://213.27.16.25:9000/video-processor/streams/stream.m3u8?camera=48&token=1&type=1",
        "Фильм+звук" to "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
        "Секундомер+звук" to "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
        "BG video+sound 720p" to "https://empireprojekt.ru/hls_mp4/bg_720p.m3u8",
        "BG video+sound 360p" to "https://empireprojekt.ru/hls_mp4/bg_360p.m3u8",
        "BG sound" to "https://empireprojekt.ru/hls_mp3/bg.m3u8"
    )

    /**
     * Индекс текущего тестового стрима
     */
    private var index = 2


    /**
     * Получение ссылки на тестовый стрим
     */
    private fun getURL(pos: Int = 0): String {
        index += pos
        if (index >= STREAMING_URLS.size)
            index = 0
        else if (index < 0)
            index = STREAMING_URLS.size - 1
        _message.value = STREAMING_URLS.keys.elementAt(index)
        return STREAMING_URLS.values.elementAt(index)
    }

    /**
     * Переключение на предыдущий стрим.
     * Привязан к кнопке.
     */
    fun prevStream() =
        hlsPlayer.setURL(getURL(-1))

    /**
     * Переключение на следующий стрим.
     * Привязан к кнопке.
     */
    fun nextStream() =
        hlsPlayer.setURL(getURL(1))


    private val _mediaUrl = MutableLiveData<String?>()
    val mediaUrl: LiveData<String?>
        get() = _mediaUrl

    /**
     * Переподключение, используемое пользователем
     */
    fun retry() {
        _message.value = "Переподключение"
        hlsPlayer.retry()
        connectionManager.checkInternetConnection()
    }

    fun setUrl(hlsUrl: String?) {
        _mediaUrl.value = hlsUrl
        hlsPlayer.setURL(_mediaUrl.value ?: "")
    }

    /**
     * Уведомления для пользователя в виде SnackBar'а
     */
    private val _message = MutableLiveData<String>(STREAMING_URLS.keys.elementAt(index))
    val message: LiveData<String>
        get() = _message

    /**
     * Ссылка на HLS Player
     */
    private val hlsPlayer: HLSPlayer = HLSPlayer(application.applicationContext, "")
    private val _player = MutableLiveData<SimpleExoPlayer?>(hlsPlayer.player)
    val player: LiveData<SimpleExoPlayer?>
        get() = _player

    /**
     * Класс для проверки интернет соединения
     */
    private val connectionManager = ConnectionManager(application.applicationContext) { msg ->
        _message.postValue(
            msg
        )
    }


    /**
     * Обработка жизненных циклов
     */
    inner class HLSPlayerObserver : LifecycleObserver {

        /**
         * После открытий необходимо перезагрузить стрим
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            hlsPlayer.retry()
        }

        /**
         * После сворачивания необходимо поставить стрим на паузу
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            hlsPlayer.pause()
        }

        /**
         * После закрытия необходимо удрать листенеры и выключить плеер
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            hlsPlayer.disable()
            connectionManager.disable()
        }

    }


}