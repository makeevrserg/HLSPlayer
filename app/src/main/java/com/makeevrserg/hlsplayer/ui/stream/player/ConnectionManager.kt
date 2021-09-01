//package com.makeevrserg.hlsplayer.ui.stream.player
//
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.Network
//import android.net.NetworkInfo
//import android.os.Build
//
///**
// * Если интернет отключился - пользователь будет уведомлен об этом
// */
//class ConnectionManager(
//    private val context: Context,
//    private val sendMessage: (str: String) -> Unit
//) {
//
//    /**
//     * Слушатель двух событий - подключение и отключение интернета.
//     */
//    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
//        override fun onAvailable(network: Network) {
//            sendMessage.invoke("Интернет доступен")
//        }
//
//        override fun onLost(network: Network) {
//            sendMessage.invoke("Интернет недоступен")
//        }
//
//    }
//
//    /**
//     * Если версия андроида позволяет - регистрируем листенер.
//     */
//    init {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            val cm = context.getSystemService(ConnectivityManager::class.java)
//            cm.registerDefaultNetworkCallback(networkCallback)
//        }
//    }
//
//
//    /**
//     * Функция для проверки интернета на старых версиях
//     */
//    fun checkInternetConnection() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//            return
//
//        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
//        if (activeNetwork?.isConnectedOrConnecting != true)
//            sendMessage.invoke("Интернет недоступен")
//
//
//    }
//
//    /**
//     * Отключение слушателя
//     */
//    fun disable() {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            val cm = context.getSystemService(ConnectivityManager::class.java)
//            cm.unregisterNetworkCallback(networkCallback)
//        }
//
//    }
//}