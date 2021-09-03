package com.makeevrserg.hlsplayer.utils

import android.content.Context
import android.content.SharedPreferences
import com.makeevrserg.hlsplayer.R
import com.makeevrserg.hlsplayer.network.cubicapi.response.UserAuthorized

class Preferences(private val context: Context) {


    private fun getSharedPrefs(): SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.shared_prefs_config),
        Context.MODE_PRIVATE
    )

    /**
     * Сохранение авторизованного пользователя
     */
    fun saveUserAuthorized(user: UserAuthorized) {
        with(getSharedPrefs().edit()) {
            putString("access_token", user.access_token)
            putString("expires_in", user.expires_in)
            putString("refresh_expires_in", user.refresh_expires_in)
            putString("refresh_token", user.refresh_token)
            putString("token_type", user.token_type)
            apply()
        }
    }

    /**
     * Получение токена пользователя
     */
    fun getToken(): String? =
        getUserAuthorized()?.access_token

    /**
     * Получение авторизованного пользователя
     */
    fun getUserAuthorized(): UserAuthorized? {
        val prefs = getSharedPrefs()
        return UserAuthorized(
            access_token = prefs.getString("access_token", null) ?: return null,
            expires_in = prefs.getString("expires_in", null) ?: return null,
            refresh_expires_in = prefs.getString("refresh_expires_in", null) ?: return null,
            refresh_token = prefs.getString("refresh_token", null) ?: return null,
            token_type = prefs.getString("token_type", null) ?: return null
        )
    }


}