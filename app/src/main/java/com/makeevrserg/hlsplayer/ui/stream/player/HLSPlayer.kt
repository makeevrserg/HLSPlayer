package com.makeevrserg.hlsplayer.ui.stream.player

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.hls.DefaultHlsExtractorFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util


class HLSPlayer(private val context: Context, private var url: String) {

    private fun getURI() = Uri.parse(url)

    private fun getUserAgent() = Util.getUserAgent(context, "HLS Player")

    /**
     * Задание MediaItem тип m3u8
     */
    private fun getHlsMediaItem() = MediaItem.Builder().setUri(getURI())
        .setMimeType(MimeTypes.APPLICATION_M3U8).build()
    /**
     * Если передается обычное URL, не HLS
     */
    private fun getMediaItem() = MediaItem.Builder().setUri(getURI()).build()

    private fun getHlsExtractorFactory() = DefaultHlsExtractorFactory(
        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES, true
    )

    private fun getDataSourceFactory() = DefaultDataSourceFactory(context, getUserAgent())

    /**
     * Создание MediaSource. setAllowChunklessPreparation для более быстрой прогрузки(как сказано в документации)
     */
    private fun getHlsMediaSource() = HlsMediaSource.Factory(getDataSourceFactory())
        .setAllowChunklessPreparation(true)
        .setExtractorFactory(getHlsExtractorFactory())
        .createMediaSource(getHlsMediaItem())

    /**
     * Если передается обычное URL, не HLS
     */
    private fun getMediaSource() = DefaultMediaSourceFactory(context).createMediaSource(getMediaItem())

    private fun getMediaSourceFactory() = DefaultMediaSourceFactory(getDataSourceFactory())

    /**
     * Создание самого плеера
     */
    private fun getExoPlayer() =
        SimpleExoPlayer.Builder(context).setMediaSourceFactory(getMediaSourceFactory())
            .build()

    /**
     * Добавление слушателя ошибок
     */
    private val listener: HLSListener = HLSListener(this)


    private var exoPlayer: SimpleExoPlayer? = getExoPlayer()
    val player: SimpleExoPlayer?
        get() = exoPlayer



    /**
     * Пауза
     */
    fun pause() {
        exoPlayer?.playWhenReady = false
        exoPlayer?.stop()
        exoPlayer?.removeListener(listener)
    }

    /**
     * Пересоздание mediaSource и включение плеера
     */
    private fun play() {
        if (url.contains("m3u8"))
            exoPlayer?.setMediaSource(getHlsMediaSource(), true)
        else
            exoPlayer?.setMediaSource(getMediaSource(),true)

        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = true
        exoPlayer?.addListener(listener)
    }

    /**
     * Перезагрузка
     */
    fun retry() {
        pause()
        play()
    }

    /**
     * Установка нового адреса стрима и перезагрузка плеера
     */
    fun setURL(url: String) {
        this.url = url
        retry()
    }

    /**
     * Перенос ползунка на default position в случае стандартных ошибок с HLS: 404, BehindLiveWindow, etc
     * @see HLSListener.onPlayerError
     */
    fun onPlayerLoadError() {
        exoPlayer?.seekToDefaultPosition()
        exoPlayer?.prepare()
    }

    /**
     * Полное отключение плеера
     */
    fun disable() {
        pause()
        exoPlayer?.release()
        exoPlayer = null
    }


}