package com.makeevrserg.hlsplayer.ui.stream.player

import com.google.android.exoplayer2.SimpleExoPlayer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

/**
 * Слушатель позиции переданного плеера.
 *
 * Позиция передается в notifier.
 */
class PlayerPositionListener(
    private val player: SimpleExoPlayer?,
    private val notifier: (Long) -> Unit
) {

    private var listener: Disposable? = null
    fun enableListener() {
        listener?.dispose()
        listener = timer.subscribe {
            notifier.invoke(it)
        }

    }

    fun disableListener() {
        listener?.dispose()
        listener = null
    }


    private val timer: Observable<Long>
        get() = Observable
            .interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                player?.currentPosition ?: 0
            }
}