package com.cloudlinks.aimeebadminton

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.util.Size
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var metrics: TextView
    private lateinit var detailPanel: LinearLayout
    private lateinit var cameraExecutor: ExecutorService
    private var frameCount = 0
    private var windowStart = 0L
    private var fps = 0.0
    private var analysisMs = 0.0
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
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
        root.addView(previewView, FrameLayout.LayoutParams(-1, -1))

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 10, 16, 10)
            setBackgroundColor(0xCC101820.toInt())
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "AIMEE BADMINTON AI · M1"
            textSize = 15f
            setTextColor(Color.WHITE)
        }
        val toggle = Button(this).apply {
            text = "详细数据"
            textSize = 11f
            setOnClickListener {
                detailPanel.visibility =
                    if (detailPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                text = if (detailPanel.visibility == View.VISIBLE) "收起" else "详细数据"
            }
        }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(toggle)
        panel.addView(header)

        val hint = TextView(this).apply {
            text = "架在底线后方的三脚架上，镜头保持水平并完整拍入球场；无需手持或蹲守。"
            textSize = 11f
            setTextColor(0xFFD5E2EF.toInt())
            setPadding(0, 4, 0, 0)
        }
        panel.addView(hint)

        detailPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 8, 0, 0)
        }
        metrics = TextView(this).apply {
            text = "正在初始化摄像头…"
            textSize = 12f
            setTextColor(0xFFE7F0FA.toInt())
        }
        detailPanel.addView(metrics)
        panel.addView(detailPanel)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ).apply { setMargins(12, 12, 12, 0) }
        root.addView(panel, params)
        setContentView(root)
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                ).build()
            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
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
                image.close()
                analysisMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                runOnUiThread { updateMetrics() }
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                metrics.text = "摄像头启动失败：${e.localizedMessage}"
                detailPanel.visibility = View.VISIBLE
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
        metrics.text = "分析帧尺寸：${imageWidth} × ${imageHeight}    分析帧率：${fps.roundToInt()} FPS\n" +
            "图像帧关闭处理耗时：${"%.2f".format(analysisMs)} ms（不代表 AI 推理耗时）\n" +
            "Java 内存：${"%.0f".format(usedMb)} / ${"%.0f".format(maxMb)} MB    设备热状态：$thermal\n" +
            "说明：当前为摄像头采集测试，尚未运行 AI 模型。"
    }

    private fun showPermissionMessage() {
        detailPanel.visibility = View.VISIBLE
        metrics.text = "需要允许摄像头权限才能进行测试。请在系统设置中为本应用开启相机权限。"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
    }
}
