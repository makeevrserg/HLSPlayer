package com.makeevrserg.hlsplayer.utils


/**
 * Одноразовый эвент
 */
open class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * Если эвент уже сработал - возвратится null
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}
