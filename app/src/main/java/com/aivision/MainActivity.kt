package com.aivision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aivision.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var camera: CameraController
    private lateinit var tts: TtsSpeaker

    private var currentMode = "describe"
    private val isLive = AtomicBoolean(false)
    private val isProcessing = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tts = TtsSpeaker(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }

        binding.btnCapture.setOnClickListener {
            if (!isLive.get()) lifecycleScope.launch { analyze() }
        }

        binding.btnLive.setOnClickListener {
            if (isLive.get()) {
                stopLive()
            } else {
                startLive()
            }
        }

        binding.btnDescribe.setOnClickListener { currentMode = "describe"; updateModeButtons() }
        binding.btnRead.setOnClickListener { currentMode = "read"; updateModeButtons() }
        binding.btnHelp.setOnClickListener { currentMode = "help"; updateModeButtons() }

        updateModeButtons()
    }

    private fun startCamera() {
        camera = CameraController(this, this, binding.previewView)
        camera.start()
    }

    private fun startLive() {
        isLive.set(true)
        binding.btnLive.text = "⏹ Стоп"
        binding.btnCapture.isEnabled = false
        lifecycleScope.launch {
            while (isLive.get()) {
                analyze()
                delay(5000)
            }
        }
    }

    private fun stopLive() {
        isLive.set(false)
        binding.btnLive.text = "🔴 Live"
        binding.btnCapture.isEnabled = true
    }

    private suspend fun analyze() {
        if (!isProcessing.compareAndSet(false, true)) return
        val jpeg = camera.latestJpeg ?: run {
            isProcessing.set(false)
            return
        }
        try {
            updateText("⏳ Анализирую...")
            val result = ApiClient.analyze(jpeg, currentMode)
            updateText(result)
            tts.speak(result)
        } catch (e: Exception) {
            updateText("Ошибка: ${e.message}")
        } finally {
            isProcessing.set(false)
        }
    }

    private fun updateText(text: String) {
        runOnUiThread { binding.tvResult.text = text }
    }

    private fun updateModeButtons() {
        binding.btnDescribe.alpha = if (currentMode == "describe") 1f else 0.5f
        binding.btnRead.alpha = if (currentMode == "read") 1f else 0.5f
        binding.btnHelp.alpha = if (currentMode == "help") 1f else 0.5f
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Нужно разрешение камеры", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLive()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
