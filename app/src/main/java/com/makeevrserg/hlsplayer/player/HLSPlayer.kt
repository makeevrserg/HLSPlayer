package com.makeevrserg.hlsplayer.player

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
     * Media item with m3u8 type
     */
    private fun getMediaItem() = MediaItem.Builder().setUri(getURI())
        .setMimeType(MimeTypes.APPLICATION_M3U8).build()




    private fun getHlsExtractorFactory() = DefaultHlsExtractorFactory(
        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES, true
    )

    private fun getDataSourceFactory() = DefaultDataSourceFactory(context,getUserAgent())
    /**
     * MediaSource buidler
     */
    private fun getHlsMediaSource() = HlsMediaSource.Factory(getDataSourceFactory())
        .setAllowChunklessPreparation(true)
        .setExtractorFactory(getHlsExtractorFactory())
        .createMediaSource(getMediaItem())

    private fun getMediaSourceFactory() = DefaultMediaSourceFactory(getDataSourceFactory())



    /**
     * ExoPlayer builder
     */
    private fun getExoPlayer() =
        SimpleExoPlayer.Builder(context).setMediaSourceFactory(getMediaSourceFactory())
            .build()



    /**
     * Player events listener
     */
    private val listener: HLSListener = HLSListener(this)

    /**
     * Player Reference
     */
    private val exoPlayer: SimpleExoPlayer = getExoPlayer()
    val player: SimpleExoPlayer
        get() = exoPlayer


    init {
        play()
    }


    /**
     * Stop streaming and remove listeners
     */
    fun stop() {
        exoPlayer.playWhenReady = false
        exoPlayer.stop()
        exoPlayer.removeListener(listener)
    }

    /**
     * Set media and play stream
     */
    fun play() {
        exoPlayer.setMediaSource(getHlsMediaSource(), true)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.addListener(listener)
    }

    /**
     * Reload player
     */
    fun retry() {
        stop()
        play()
    }

    /**
     * Set new url for stream and reload player
     */
    fun setURL(url: String) {
        this.url = url
        retry()
    }

    /**
     * Seek player to default position in case of errors 404, BehindLiveWindow, etc
     */
    fun reseek() {
        exoPlayer.seekToDefaultPosition()
        exoPlayer.prepare()
    }


}