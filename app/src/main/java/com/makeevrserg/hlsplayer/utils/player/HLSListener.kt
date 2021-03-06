package com.makeevrserg.hlsplayer.utils.player

import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.hls.HlsManifest

/**
 * Обработчик ошибок ExoPlayer
 */
class HLSListener(val hlsPlayer: HLSPlayer) : Player.Listener {
    /**
     * В плеере могут возникнуть ошибки, связанные с HLS.
     * В таком случае необходимо будет сбросить позицию плеера.
     * В документации указана только ERROR_CODE_BEHIND_LIVE_WINDOW, однако могут возникнуть еще несколько, включая 404, которые учтены здесь.
     */
    override fun onPlayerError(error: PlaybackException) {
        if (listOf(
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
                PlaybackException.ERROR_CODE_UNSPECIFIED,
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            ).contains(error.errorCode)
        )
            hlsPlayer.onPlayerLoadError()
        //Эта ошибка может случиться из-за отсутствия интернета
        else if (PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED == error.errorCode)
            hlsPlayer.onPlayerLoadError()
    }


    /**
     * Мониторим переключение на другой медиа файл
     */
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        hlsPlayer.onMediaItemTransition()
    }


    /**
     * Без использования, но можно читать манифест (Если нужно)
     */
    override fun onTimelineChanged(_timeline: Timeline, reason: Int) {
        val manifest = (hlsPlayer.player?.currentManifest ?: return) as HlsManifest
    }


}