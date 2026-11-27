package com.cloudlinks.aimeebadminton

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var metrics: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var frameCount = 0
    private var windowStart = 0L
    private var lastFrameTime = 0L
    private var fps = 0.0
    private var lastAnalysisMs = 0.0
    private var imageWidth = 0
    private var imageHeight = 0
    private var thermal = "系统未提供"

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else showPermissionMessage()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        buildUi()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun buildUi() {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        root.addView(previewView, FrameLayout.LayoutParams(-1, -1))

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 22, 28, 22)
            setBackgroundColor(0xCC101820.toInt())
        }
        val title = TextView(this).apply {
            text = "AIMEE BADMINTON AI  ·  M1"
            textSize = 17f
            setTextColor(Color.WHITE)
        }
        metrics = TextView(this).apply {
            text = "正在初始化摄像头…"
            textSize = 14f
            setTextColor(0xFFE7F0FA.toInt())
            setPadding(0, 14, 0, 0)
        }
        panel.addView(title)
        panel.addView(metrics)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ).apply { setMargins(18, 24, 18, 0) }
        root.addView(panel, params)
        setContentView(root)
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
            analysis.setAnalyzer(cameraExecutor) { image ->
                val start = SystemClock.elapsedRealtimeNanos()
                imageWidth = image.width
                imageHeight = image.height
                val now = SystemClock.elapsedRealtime()
                if (windowStart == 0L) windowStart = now
                frameCount++
                if (now - windowStart >= 2000L) {
                    fps = frameCount * 1000.0 / (now - windowStart)
                    frameCount = 0
                    windowStart = now
                }
                if (lastFrameTime != 0L) {
                    // Keep the latest frame interval as an additional responsiveness indicator.
                }
                lastFrameTime = now
                lastAnalysisMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                image.close()
                runOnUiThread { updateMetrics() }
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                metrics.text = "摄像头启动失败：${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateMetrics() {
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
        val maxMb = runtime.maxMemory() / (1024.0 * 1024.0)
        if (Build.VERSION.SDK_INT >= 29) {
            thermal = when (getSystemService(android.os.PowerManager::class.java).currentThermalStatus) {
                0 -> "正常"
                1 -> "轻微"
                2 -> "中等"
                3 -> "严重"
                4 -> "危急"
                5 -> "关闭"
                6 -> "紧急"
                else -> "未知"
            }
        }
        metrics.text = "摄像头：后置主摄（系统默认）\n" +
            "分析帧尺寸：${imageWidth} × ${imageHeight}\n" +
            "实时分析帧率：${fps.roundToInt()} FPS\n" +
            "单帧分析耗时：${"%.2f".format(lastAnalysisMs)} ms\n" +
            "Java 内存：${"%.0f".format(usedMb)} / ${"%.0f".format(maxMb)} MB\n" +
            "设备热状态：$thermal\n\n" +
            "说明：当前为摄像头采集基准测试，尚未运行 AI 模型。"
    }

    private fun showPermissionMessage() {
        metrics.text = "需要允许摄像头权限才能进行测试。请在系统设置中为本应用开启相机权限。"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
    }
}
