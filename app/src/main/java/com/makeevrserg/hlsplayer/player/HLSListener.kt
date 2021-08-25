package com.makeevrserg.hlsplayer.player

import android.util.Log
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.hls.HlsManifest

/**
 * Error handler for ExoPlayer
 */
class HLSListener(val hlsPlayer: HLSPlayer) : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
        if (listOf(
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
                PlaybackException.ERROR_CODE_UNSPECIFIED,
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
            ).contains(error.errorCode)
        )
            hlsPlayer.reseek()


    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        val manifest = hlsPlayer.player.currentManifest as HlsManifest

    }


}