package com.mwysocki.smartwarehouse.viewmodels

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.*
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedViewModel : ViewModel() {
    private val _timeLeft = MutableStateFlow(2 * 60) // 2 minutes
    val timeLeft: StateFlow<Int> = _timeLeft

    private var timer: CountDownTimer? = null

    private val _triggerRecomposition = MutableStateFlow(0)
    val triggerRecomposition: StateFlow<Int> = _triggerRecomposition

    private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
    val qrCodeBitmap: StateFlow<Bitmap?> = _qrCodeBitmap

    fun startTimer() {
        timer?.cancel() // Cancel any existing timer
        _timeLeft.value = 2 * 60 // Reset the timer value to 2 minutes

        _triggerRecomposition.value += 1

        timer = object : CountDownTimer(_timeLeft.value.toLong() * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeft.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _timeLeft.value = 0
                _qrCodeBitmap.value = null
                timer = null
                onCountdownFinished()
            }
        }.start()
    }

    fun isTimerRunning(): Boolean = timer != null

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }

    fun onCountdownFinished() {
        _triggerRecomposition.value += 1
    }

    fun generateQRCode(userId: String) {
        val currentTime = Calendar.getInstance().time
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val twoMinutesLater = Date(currentTime.time + 2 * 60 * 1000)
        val qrData = "$userId - ${sdf.format(currentTime)} to ${sdf.format(twoMinutesLater)}"

        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            _qrCodeBitmap.value = bmp
        } catch (e: Exception) {
            Log.e("SharedViewModel", "Error generating QR code", e)
        }
    }
}