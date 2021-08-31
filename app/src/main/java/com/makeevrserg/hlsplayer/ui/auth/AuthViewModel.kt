package com.makeevrserg.hlsplayer.ui.auth

import android.app.Application
import android.content.Context
import android.system.ErrnoException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.makeevrserg.hlsplayer.network.cubicapi.CubicAPI
import com.makeevrserg.hlsplayer.network.cubicapi.response.Camera
import com.makeevrserg.hlsplayer.network.cubicapi.response.CameraItem
import com.makeevrserg.hlsplayer.network.cubicapi.response.camera.timestamp.CameraFileTimestamps
import com.makeevrserg.hlsplayer.utils.Preferences
import com.makeevrserg.hlsplayer.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException

class AuthViewModel(application: Application) : AndroidViewModel(application) {


    val TAG = "AuthViewModel"


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private fun startLoading(){
        _isLoading.postValue(true)
    }
    fun doneLoading(){
        _isLoading.postValue(false)
    }

    /**
     * Сообщение, которые выводится пользователю в SnackBar'е чтобы оповестить его о чем-либо
     */
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?>
        get() = _message

    private fun startShowMessage(msg: String) {
        _message.postValue(msg)
    }

    fun stopShowMessage() {
        _message.postValue(null)
    }

    /**
     * Для дебага
     */
    private val _logMessage = MutableLiveData<String>()
    val logMessage: LiveData<String>
        get() = _logMessage


    /**
     * Список доступных камер
     */
    private val _cameras = MutableLiveData<Camera>()
    val cameras: LiveData<Camera>
        get() = _cameras


    /**
     * Ссылка на HLS стрим выбранной камеры пользователем
     */
    private val _mediaURL = MutableLiveData<String?>()
    val mediaURL: LiveData<String?>
        get() = _mediaURL

    /**
     * Ссылка на объект выбранной камеры пользователем из списка RecyclerView
     */
    private var selectedCamera: CameraItem? = null

    /**
     * Список доступных timestamp'ов для камеры
     */
    private val _cameraTimestamps = MutableLiveData<CameraFileTimestamps>()
    val cameraTimestamps: LiveData<CameraFileTimestamps>
        get() = _cameraTimestamps

    /**
     * При нажатии на item камеры из RecyclerView показывается диалог с возможностью выбора стрима либо периода
     */
    private val _selectCameraDialog = MutableLiveData<Boolean>()
    val selectCameraDialog: LiveData<Boolean>
        get() = _selectCameraDialog

    private fun startShowCameraDialog() {
        _selectCameraDialog.value = true
    }

    fun stopShowCameraDialog() {
        _selectCameraDialog.value = false
    }

    /**
     * Обновление токена пользователя в билдере Retrofit'а
     */
    private fun updateToken(context: Context): Unit =
        CubicAPI.updateToken(Preferences(context).getToken())

    init {
        updateToken(application)
    }


    fun onAuthClicked(login: String?, password: String?) {
        if (login.isNullOrEmpty() || password.isNullOrEmpty()) {
            startShowMessage("Не введены данные авторизации")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            startLoading()
            try {
                val user = CubicAPI.retrofitService.loginUser(login, password).await()
                Preferences(getApplication()).saveUserAuthorized(user)
                startShowMessage("Успешная авторизация")
                updateToken(getApplication())
                _logMessage.postValue(user.toString())
            } catch (e: HttpException) {
                startShowMessage("Не удалось авторизоваться")
            } catch (e: ConnectException) {
                startShowMessage("Не удалось подключиться")

            } catch (e: ErrnoException) {
                startShowMessage("Не удалось подключиться")

            } catch (e: SocketTimeoutException) {
                startShowMessage("Нет подключения к серверу")
            }
            doneLoading()
        }
    }

    fun onUserInfoClicked() {
        Log.d(TAG, "onUserInfoClicked: ")
        viewModelScope.launch(Dispatchers.IO) {
            startLoading()
            try {
                val userInfo = CubicAPI.retrofitService.getUserInfo().await()
                _logMessage.postValue(userInfo.toString())
            } catch (e: HttpException) {
                startShowMessage("Вы не авторизованы")
            } catch (e: ConnectException) {
                startShowMessage("Не удалось подключиться")
            } catch (e: ErrnoException) {
                startShowMessage("Не удалось подключиться")

            } catch (e: SocketTimeoutException) {
                startShowMessage("Нет подключения к серверу")
            }
            doneLoading()
        }
    }

    fun onUserLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            startLoading()
            try {
                val user = Preferences(getApplication()).getUserAuthorized()
                val userInfo = CubicAPI.retrofitService.logout(user?.refresh_token).await()
                startShowMessage("Вы вышли")
                _logMessage.postValue(userInfo.toString())
                _cameras.value?.clear()
            } catch (e: HttpException) {
                startShowMessage("Вы не авторизованы")
            } catch (e: ConnectException) {
                startShowMessage("Не удалось подключиться")
            } catch (e: ErrnoException) {
                startShowMessage("Не удалось подключиться")
            } catch (e: SocketTimeoutException) {
                startShowMessage("Нет подключения к серверу")
            }
            doneLoading()
        }
    }

    fun onCamerasClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            startLoading()
            try {
                val cameras = CubicAPI.retrofitService.getCameras().await()
                _logMessage.postValue(cameras.toString())
                _cameras.postValue(cameras)
            } catch (e: HttpException) {
                startShowMessage("Вы не авторизованы")
            } catch (e: ConnectException) {
                startShowMessage("Не удалось подключиться")
            } catch (e: ErrnoException) {
                startShowMessage("Не удалось подключиться")
            } catch (e: SocketTimeoutException) {
                startShowMessage("Нет подключения к серверу")
            }
            doneLoading()
        }
    }


    /**
     * onClickListener of RecyclerView
     */
    fun onOpenCameraDialog(camera: CameraItem) {
        startShowCameraDialog()
        selectedCamera = camera
    }

    /**
     * Пользователь выбрал дату, за которую хочет посмотреть видео с камер
     */
    fun onDateSelected(year: Int, month: Int, day: Int) {
        val timeStamp = "$year-${month + 1}-$day"
        val cameraId = selectedCamera?.id
        viewModelScope.launch(Dispatchers.IO) {
            startLoading()
            try {
                println(cameraId)
                println(timeStamp)
                val cameraTimestamps =
                    CubicAPI.retrofitService.getVideoByTimestamp(cameraId, timeStamp).await()

                _cameraTimestamps.postValue(cameraTimestamps)

            } catch (e: HttpException) {
                startShowMessage("Нет видео по этой дате")
            } catch (e: ConnectException) {
                startShowMessage("Не удалось подключиться")
            } catch (e: ErrnoException) {
                startShowMessage("Не удалось подключиться")
            } catch (e: SocketTimeoutException) {
                startShowMessage("Нет подключения к серверу")
            }
            doneLoading()
        }


    }

    fun onLiveStreamSelected() {
        _mediaURL.value = Utils.getHlsUrl(selectedCamera?.id ?: return, getApplication())
    }

    fun doneShowUrl() {
        _mediaURL.value = null
    }

    fun onTimestampSelected(position: Int) {
        _mediaURL.value =
            Utils.getFileUrl(_cameraTimestamps.value?.elementAtOrNull(position)?.file ?: "")
    }


}