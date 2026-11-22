package tech.huangsh.firstsight.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import tech.huangsh.firstsight.data.model.Settings
import tech.huangsh.firstsight.ui.screens.tools.ToolsScreen
import tech.huangsh.firstsight.ui.theme.FirstSightTheme
import tech.huangsh.firstsight.viewmodel.SettingsViewModel
import tech.huangsh.firstsight.viewmodel.ToolsViewModel

@AndroidEntryPoint
class ToolsActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TOOL = "extra_tool"
        const val TOOL_FLASHLIGHT = "flashlight"
        const val TOOL_MAGNIFIER = "magnifier"
        const val TOOL_CALCULATOR = "calculator"

        fun createIntent(context: Context, tool: String? = null): Intent {
            return Intent(context, ToolsActivity::class.java).apply {
                tool?.let { putExtra(EXTRA_TOOL, it) }
            }
        }
    }

    private val viewModel: ToolsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialTool = intent.getStringExtra(EXTRA_TOOL)
        setContent {
            val settings by settingsViewModel.settings.collectAsState(initial = Settings())
            FirstSightTheme(themeMode = settings.themeMode) {
                ToolsScreen(
                    initialTool = initialTool,
                    onBack = { finish() },
                    viewModel = viewModel
                )
            }
        }
    }
}
