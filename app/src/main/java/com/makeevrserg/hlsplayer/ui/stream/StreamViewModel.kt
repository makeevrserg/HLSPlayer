package com.makeevrserg.hlsplayer.ui.stream

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.android.exoplayer2.SimpleExoPlayer
import com.makeevrserg.hlsplayer.player.HLSPlayer


class StreamViewModel(application: Application) : AndroidViewModel(application) {


    val STREAMING_URLS = listOf(
        "https://moctobpltc-i.akamaihd.net/hls/live/571329/eight/playlist.m3u8",
        "http://213.27.16.25:9000/video-processor/streams/stream.m3u8?camera=3&token=1&type=1",
        "http://213.27.16.25:9000/video-processor/streams/stream.m3u8?camera=48&token=1&type=1",

        "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
        "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"
    )
    var index = 0
    private fun getURL(): String {
        if (index >= STREAMING_URLS.size)
            index = 0
        else if (index < 0)
            index = STREAMING_URLS.size - 1
        return STREAMING_URLS[index]
    }


    private val _player = MutableLiveData<SimpleExoPlayer>()
    val player: LiveData<SimpleExoPlayer>
        get() = _player

    private var hlsPlayer: HLSPlayer = HLSPlayer(application.applicationContext, getURL())

    init {
        _player.value = hlsPlayer.player

    }


    /**
     * Handling fragment lifecycles
     */
    inner class HLSPlayerObserver : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            _player.value = hlsPlayer.player
            hlsPlayer.retry()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            hlsPlayer.stop()
        }
    }


    fun retry() {
        hlsPlayer.retry()
    }

    fun prevStream() {
        index -= 1
        hlsPlayer.setURL(getURL())
    }


    fun nextStream() {
        index += 1
        hlsPlayer.setURL(getURL())

    }

}