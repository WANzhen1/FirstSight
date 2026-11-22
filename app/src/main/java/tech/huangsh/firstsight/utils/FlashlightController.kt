package tech.huangsh.firstsight.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class FlashlightResult {
    ON,
    OFF,
    NO_FLASH,
    PERMISSION_DENIED,
    ERROR
}

@Singleton
class FlashlightController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val cameraId: String? = runCatching {
        cameraManager?.cameraIdList?.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    val hasFlashlight: Boolean
        get() = cameraId != null

    fun toggle(): FlashlightResult = setTorch(!_isOn.value)

    fun setTorch(on: Boolean): FlashlightResult {
        val id = cameraId ?: return FlashlightResult.NO_FLASH
        val manager = cameraManager ?: return FlashlightResult.NO_FLASH
        return try {
            manager.setTorchMode(id, on)
            _isOn.value = on
            if (on) FlashlightResult.ON else FlashlightResult.OFF
        } catch (e: SecurityException) {
            FlashlightResult.PERMISSION_DENIED
        } catch (e: Exception) {
            FlashlightResult.ERROR
        }
    }
}
