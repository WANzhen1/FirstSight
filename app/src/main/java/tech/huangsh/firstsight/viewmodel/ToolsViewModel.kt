package tech.huangsh.firstsight.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.huangsh.firstsight.utils.FlashlightController
import tech.huangsh.firstsight.utils.FlashlightResult
import javax.inject.Inject

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val flashlightController: FlashlightController
) : ViewModel() {
    val flashlightOn = flashlightController.isOn
    val hasFlashlight: Boolean get() = flashlightController.hasFlashlight

    fun toggleFlashlight(): FlashlightResult = flashlightController.toggle()
}
