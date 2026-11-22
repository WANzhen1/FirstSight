package tech.huangsh.firstsight.ui.screens.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import tech.huangsh.firstsight.R
import tech.huangsh.firstsight.utils.ToastUtils
import kotlin.math.abs

@Composable
fun MagnifierView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    if (!hasCameraPermission) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.tool_magnifier_permission),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text(stringResource(R.string.tool_open_camera_permission), fontSize = 17.sp)
            }
        }
        return
    }

    var cameraView by remember { mutableStateOf<MagnifierTextureView?>(null) }
    var zoom by remember { mutableStateOf(0) }
    var maxZoom by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            cameraView?.releaseCamera()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MagnifierTextureView(ctx).also { view ->
                    view.onReady = {
                        maxZoom = view.maxZoom
                    }
                    view.onError = {
                        ToastUtils.show(ctx, R.string.tool_camera_unavailable)
                    }
                    cameraView = view
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = stringResource(R.string.tool_zoom, 1f + zoom),
                color = Color.White,
                fontSize = 16.sp
            )
            Slider(
                value = zoom.toFloat(),
                onValueChange = { value ->
                    zoom = value.toInt()
                    cameraView?.setZoom(zoom)
                },
                valueRange = 0f..maxZoom.coerceAtLeast(1).toFloat()
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
private class MagnifierTextureView(context: Context) :
    TextureView(context), TextureView.SurfaceTextureListener {

    private var camera: Camera? = null
    var maxZoom: Int = 0
        private set
    var onReady: (() -> Unit)? = null
    var onError: (() -> Unit)? = null

    init {
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        openCamera(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        releaseCamera()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    private fun openCamera(surface: SurfaceTexture) {
        releaseCamera()
        runCatching {
            val opened = Camera.open()
            val params = opened.parameters
            val bestSize = params.supportedPreviewSizes
                ?.minByOrNull { abs(it.width - 1080) + abs(it.height - 1440) }
            bestSize?.let { params.setPreviewSize(it.width, it.height) }
            if (params.isZoomSupported) {
                maxZoom = params.maxZoom
                params.zoom = 0
            }
            opened.parameters = params
            opened.setDisplayOrientation(90)
            opened.setPreviewTexture(surface)
            opened.startPreview()
            camera = opened
            onReady?.invoke()
        }.onFailure {
            releaseCamera()
            onError?.invoke()
        }
    }

    fun setZoom(value: Int) {
        val opened = camera ?: return
        runCatching {
            val params = opened.parameters
            if (params.isZoomSupported) {
                params.zoom = value.coerceIn(0, params.maxZoom)
                opened.parameters = params
            }
        }
    }

    fun releaseCamera() {
        camera?.let { opened ->
            runCatching { opened.stopPreview() }
            runCatching { opened.release() }
        }
        camera = null
    }
}
