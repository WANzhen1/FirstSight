package tech.huangsh.firstsight.ui.theme

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 项目中用到的 Material 扩展图标集合（内联标准 24dp 矢量路径）
 *
 * 替代全量 material-icons-extended 依赖，避免将数千个未使用的图标打入 APK，
 * 可减少约 35~40 MB 原始 DEX 体积。
 */
object FirstSightIcons {

    val Android: ImageVector by lazy {
        materialIcon(name = "Filled.Android") {
            materialPath {
                moveTo(6.0f, 18.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(1.0f)
                verticalLineToRelative(3.5f)
                curveToRelative(0.0f, 0.83f, 0.67f, 1.5f, 1.5f, 1.5f)
                reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                verticalLineTo(19.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(3.5f)
                curveToRelative(0.0f, 0.83f, 0.67f, 1.5f, 1.5f, 1.5f)
                reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                verticalLineTo(19.0f)
                horizontalLineToRelative(1.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineTo(8.0f)
                horizontalLineTo(6.0f)
                verticalLineTo(18.0f)
                close()
                moveTo(3.5f, 8.0f)
                curveTo(2.67f, 8.0f, 2.0f, 8.67f, 2.0f, 9.5f)
                verticalLineToRelative(7.0f)
                curveTo(2.0f, 17.33f, 2.67f, 18.0f, 3.5f, 18.0f)
                reflectiveCurveTo(5.0f, 17.33f, 5.0f, 16.5f)
                verticalLineToRelative(-7.0f)
                curveTo(5.0f, 8.67f, 4.33f, 8.0f, 3.5f, 8.0f)
                close()
                moveTo(20.5f, 8.0f)
                curveToRelative(-0.83f, 0.0f, -1.5f, 0.67f, -1.5f, 1.5f)
                verticalLineToRelative(7.0f)
                curveToRelative(0.0f, 0.83f, 0.67f, 1.5f, 1.5f, 1.5f)
                reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                verticalLineToRelative(-7.0f)
                curveTo(22.0f, 8.67f, 21.33f, 8.0f, 20.5f, 8.0f)
                close()
                moveTo(15.53f, 2.16f)
                lineToRelative(1.3f, -1.3f)
                curveToRelative(0.2f, -0.2f, 0.2f, -0.51f, 0.0f, -0.71f)
                curveToRelative(-0.2f, -0.2f, -0.51f, -0.2f, -0.71f, 0.0f)
                lineToRelative(-1.48f, 1.48f)
                curveTo(13.85f, 1.23f, 12.95f, 1.0f, 12.0f, 1.0f)
                curveToRelative(-0.96f, 0.0f, -1.86f, 0.23f, -2.66f, 0.63f)
                lineTo(7.85f, 0.15f)
                curveToRelative(-0.2f, -0.2f, -0.51f, -0.2f, -0.71f, 0.0f)
                curveToRelative(-0.2f, 0.2f, -0.2f, 0.51f, 0.0f, 0.71f)
                lineToRelative(1.31f, 1.31f)
                curveTo(6.97f, 3.26f, 6.0f, 5.01f, 6.0f, 7.0f)
                horizontalLineToRelative(12.0f)
                curveTo(18.0f, 5.01f, 17.03f, 3.25f, 15.53f, 2.16f)
                close()
                moveTo(10.0f, 5.0f)
                horizontalLineTo(9.0f)
                verticalLineTo(4.0f)
                horizontalLineToRelative(1.0f)
                verticalLineTo(5.0f)
                close()
                moveTo(15.0f, 5.0f)
                horizontalLineToRelative(-1.0f)
                verticalLineTo(4.0f)
                horizontalLineToRelative(1.0f)
                verticalLineTo(5.0f)
                close()
            }
        }
    }

    val Apps: ImageVector by lazy {
        materialIcon(name = "Filled.Apps") {
            materialPath {
                moveTo(4.0f, 8.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(4.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(8.0f)
                close()
                moveTo(10.0f, 20.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(20.0f)
                close()
                moveTo(4.0f, 20.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(20.0f)
                close()
                moveTo(4.0f, 14.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(14.0f)
                close()
                moveTo(10.0f, 14.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(14.0f)
                close()
                moveTo(16.0f, 4.0f)
                verticalLineToRelative(4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(4.0f)
                horizontalLineTo(16.0f)
                close()
                moveTo(10.0f, 8.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(4.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(8.0f)
                close()
                moveTo(16.0f, 14.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(14.0f)
                close()
                moveTo(16.0f, 20.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(20.0f)
                close()
            }
        }
    }

    val Autorenew: ImageVector by lazy {
        materialIcon(name = "Filled.Autorenew") {
            materialPath {
                moveTo(12.0f, 6.0f)
                verticalLineToRelative(3.0f)
                lineToRelative(4.0f, -4.0f)
                lineToRelative(-4.0f, -4.0f)
                verticalLineToRelative(3.0f)
                curveToRelative(-4.42f, 0.0f, -8.0f, 3.58f, -8.0f, 8.0f)
                curveToRelative(0.0f, 1.57f, 0.46f, 3.03f, 1.24f, 4.26f)
                lineTo(6.7f, 14.8f)
                curveTo(6.25f, 13.97f, 6.0f, 13.01f, 6.0f, 12.0f)
                curveToRelative(0.0f, -3.31f, 2.69f, -6.0f, 6.0f, -6.0f)
                close()
                moveTo(18.76f, 7.74f)
                lineTo(17.3f, 9.2f)
                curveToRelative(0.44f, 0.84f, 0.7f, 1.79f, 0.7f, 2.8f)
                curveToRelative(0.0f, 3.31f, -2.69f, 6.0f, -6.0f, 6.0f)
                verticalLineToRelative(-3.0f)
                lineToRelative(-4.0f, 4.0f)
                lineToRelative(4.0f, 4.0f)
                verticalLineToRelative(-3.0f)
                curveToRelative(4.42f, 0.0f, 8.0f, -3.58f, 8.0f, -8.0f)
                curveToRelative(0.0f, -1.57f, -0.46f, -3.03f, -1.24f, -4.26f)
                close()
            }
        }
    }

    val BatterySaver: ImageVector by lazy {
        materialIcon(name = "Filled.BatterySaver") {
            materialPath {
                moveTo(16.0f, 4.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineTo(2.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(8.0f)
                curveTo(7.45f, 4.0f, 7.0f, 4.45f, 7.0f, 5.0f)
                verticalLineToRelative(16.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(8.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineTo(5.0f)
                curveTo(17.0f, 4.45f, 16.55f, 4.0f, 16.0f, 4.0f)
                close()
                moveTo(15.0f, 14.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(9.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(14.0f)
                close()
            }
        }
    }

    val CameraAlt: ImageVector by lazy {
        materialIcon(name = "Filled.CameraAlt") {
            materialPath {
                moveTo(12.0f, 12.0f)
                moveToRelative(-3.2f, 0.0f)
                arcToRelative(3.2f, 3.2f, 0.0f, true, true, 6.4f, 0.0f)
                arcToRelative(3.2f, 3.2f, 0.0f, true, true, -6.4f, 0.0f)
            }
            materialPath {
                moveTo(9.0f, 2.0f)
                lineTo(7.17f, 4.0f)
                horizontalLineTo(4.0f)
                curveTo(2.9f, 4.0f, 2.0f, 4.9f, 2.0f, 6.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(6.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                horizontalLineToRelative(-3.17f)
                lineTo(15.0f, 2.0f)
                horizontalLineTo(9.0f)
                close()
                moveTo(12.0f, 17.0f)
                curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
                reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
                reflectiveCurveToRelative(-2.24f, 5.0f, -5.0f, 5.0f)
                close()
            }
        }
    }

    val DisplaySettings: ImageVector by lazy {
        materialIcon(name = "Filled.DisplaySettings") {
            materialPath {
                moveTo(20.0f, 3.0f)
                horizontalLineTo(4.0f)
                curveTo(2.89f, 3.0f, 2.0f, 3.89f, 2.0f, 5.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(4.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(5.0f)
                curveTo(22.0f, 3.89f, 21.1f, 3.0f, 20.0f, 3.0f)
                close()
                moveTo(20.0f, 17.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(16.0f)
                verticalLineTo(17.0f)
                close()
            }
            materialPath {
                moveTo(6.0f, 8.25f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(1.5f)
                horizontalLineToRelative(-8.0f)
                close()
            }
            materialPath {
                moveTo(16.5f, 9.75f)
                lineToRelative(1.5f, 0.0f)
                lineToRelative(0.0f, -1.5f)
                lineToRelative(-1.5f, 0.0f)
                lineToRelative(0.0f, -1.25f)
                lineToRelative(-1.5f, 0.0f)
                lineToRelative(0.0f, 4.0f)
                lineToRelative(1.5f, 0.0f)
                close()
            }
            materialPath {
                moveTo(10.0f, 12.25f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(1.5f)
                horizontalLineToRelative(-8.0f)
                close()
            }
            materialPath {
                moveTo(7.5f, 15.0f)
                lineToRelative(1.5f, 0.0f)
                lineToRelative(0.0f, -4.0f)
                lineToRelative(-1.5f, 0.0f)
                lineToRelative(0.0f, 1.25f)
                lineToRelative(-1.5f, 0.0f)
                lineToRelative(0.0f, 1.5f)
                lineToRelative(1.5f, 0.0f)
                close()
            }
        }
    }

    val DragIndicator: ImageVector by lazy {
        materialIcon(name = "Filled.DragIndicator") {
            materialPath {
                moveTo(11.0f, 18.0f)
                curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                reflectiveCurveToRelative(-2.0f, -0.9f, -2.0f, -2.0f)
                reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
                reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
                close()
                moveTo(9.0f, 10.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(9.0f, 4.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(15.0f, 8.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                close()
                moveTo(15.0f, 10.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(15.0f, 16.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
            }
        }
    }

    val Image: ImageVector by lazy {
        materialIcon(name = "Filled.Image") {
            materialPath {
                moveTo(21.0f, 19.0f)
                verticalLineTo(5.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                horizontalLineTo(5.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                close()
                moveTo(8.5f, 13.5f)
                lineToRelative(2.5f, 3.01f)
                lineTo(14.5f, 12.0f)
                lineToRelative(4.5f, 6.0f)
                horizontalLineTo(5.0f)
                lineToRelative(3.5f, -4.5f)
                close()
            }
        }
    }

    val ImportContacts: ImageVector by lazy {
        materialIcon(name = "Filled.ImportContacts") {
            materialPath {
                moveTo(17.5f, 4.5f)
                curveToRelative(-1.95f, 0.0f, -4.05f, 0.4f, -5.5f, 1.5f)
                curveToRelative(-1.45f, -1.1f, -3.55f, -1.5f, -5.5f, -1.5f)
                reflectiveCurveTo(2.45f, 4.9f, 1.0f, 6.0f)
                verticalLineToRelative(14.65f)
                curveToRelative(0.0f, 0.25f, 0.25f, 0.5f, 0.5f, 0.5f)
                curveToRelative(0.1f, 0.0f, 0.15f, -0.05f, 0.25f, -0.05f)
                curveTo(3.1f, 20.45f, 5.05f, 20.0f, 6.5f, 20.0f)
                curveToRelative(1.95f, 0.0f, 4.05f, 0.4f, 5.5f, 1.5f)
                curveToRelative(1.35f, -0.85f, 3.8f, -1.5f, 5.5f, -1.5f)
                curveToRelative(1.65f, 0.0f, 3.35f, 0.3f, 4.75f, 1.05f)
                curveToRelative(0.1f, 0.05f, 0.15f, 0.05f, 0.25f, 0.05f)
                curveToRelative(0.25f, 0.0f, 0.5f, -0.25f, 0.5f, -0.5f)
                verticalLineTo(6.0f)
                curveToRelative(-1.49f, -1.12f, -3.63f, -1.5f, -5.5f, -1.5f)
                close()
                moveTo(21.0f, 18.5f)
                curveToRelative(-1.1f, -0.35f, -2.3f, -0.5f, -3.5f, -0.5f)
                curveToRelative(-1.7f, 0.0f, -4.15f, 0.65f, -5.5f, 1.5f)
                verticalLineTo(8.0f)
                curveToRelative(1.35f, -0.85f, 3.8f, -1.5f, 5.5f, -1.5f)
                curveToRelative(1.2f, 0.0f, 2.4f, 0.15f, 3.5f, 0.5f)
                verticalLineTo(18.5f)
                close()
            }
        }
    }

    val Layers: ImageVector by lazy {
        materialIcon(name = "Filled.Layers") {
            materialPath {
                moveTo(11.99f, 18.54f)
                lineToRelative(-7.37f, -5.73f)
                lineTo(3.0f, 14.07f)
                lineToRelative(9.0f, 7.0f)
                lineToRelative(9.0f, -7.0f)
                lineToRelative(-1.63f, -1.27f)
                lineToRelative(-7.38f, 5.74f)
                close()
                moveTo(12.0f, 16.0f)
                lineToRelative(7.36f, -5.73f)
                lineTo(21.0f, 9.0f)
                lineToRelative(-9.0f, -7.0f)
                lineToRelative(-9.0f, 7.0f)
                lineToRelative(1.63f, 1.27f)
                lineTo(12.0f, 16.0f)
                close()
            }
        }
    }

    val MyLocation: ImageVector by lazy {
        materialIcon(name = "Filled.MyLocation") {
            materialPath {
                moveTo(12.0f, 8.0f)
                curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
                reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
                reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f)
                close()
                moveTo(20.94f, 11.0f)
                curveToRelative(-0.46f, -4.17f, -3.77f, -7.48f, -7.94f, -7.94f)
                verticalLineTo(1.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.06f)
                curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11.0f)
                horizontalLineTo(1.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.06f)
                curveToRelative(0.46f, 4.17f, 3.77f, 7.48f, 7.94f, 7.94f)
                verticalLineTo(23.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-2.06f)
                curveToRelative(4.17f, -0.46f, 7.48f, -3.77f, 7.94f, -7.94f)
                horizontalLineTo(23.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-2.06f)
                close()
                moveTo(12.0f, 19.0f)
                curveToRelative(-3.87f, 0.0f, -7.0f, -3.13f, -7.0f, -7.0f)
                reflectiveCurveToRelative(3.13f, -7.0f, 7.0f, -7.0f)
                reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f)
                reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f)
                close()
            }
        }
    }

    val Palette: ImageVector by lazy {
        materialIcon(name = "Filled.Palette") {
            materialPath {
                moveTo(12.0f, 3.0f)
                curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
                reflectiveCurveToRelative(4.03f, 9.0f, 9.0f, 9.0f)
                curveToRelative(0.83f, 0.0f, 1.5f, -0.67f, 1.5f, -1.5f)
                curveToRelative(0.0f, -0.39f, -0.15f, -0.74f, -0.39f, -1.01f)
                curveToRelative(-0.23f, -0.26f, -0.38f, -0.61f, -0.38f, -0.99f)
                curveToRelative(0.0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
                horizontalLineTo(16.0f)
                curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
                curveToRelative(0.0f, -4.42f, -4.03f, -8.0f, -9.0f, -8.0f)
                close()
                moveTo(6.5f, 12.0f)
                curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(5.67f, 9.0f, 6.5f, 9.0f)
                reflectiveCurveTo(8.0f, 9.67f, 8.0f, 10.5f)
                reflectiveCurveTo(7.33f, 12.0f, 6.5f, 12.0f)
                close()
                moveTo(9.5f, 8.0f)
                curveTo(8.67f, 8.0f, 8.0f, 7.33f, 8.0f, 6.5f)
                reflectiveCurveTo(8.67f, 5.0f, 9.5f, 5.0f)
                reflectiveCurveTo(11.0f, 5.67f, 11.0f, 6.5f)
                reflectiveCurveTo(10.33f, 8.0f, 9.5f, 8.0f)
                close()
                moveTo(14.5f, 8.0f)
                curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(13.67f, 5.0f, 14.5f, 5.0f)
                reflectiveCurveTo(16.0f, 5.67f, 16.0f, 6.5f)
                reflectiveCurveTo(15.33f, 8.0f, 14.5f, 8.0f)
                close()
                moveTo(17.5f, 12.0f)
                curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(16.67f, 9.0f, 17.5f, 9.0f)
                reflectiveCurveTo(19.0f, 9.67f, 19.0f, 10.5f)
                reflectiveCurveTo(18.33f, 12.0f, 17.5f, 12.0f)
                close()
            }
        }
    }

    val People: ImageVector by lazy {
        materialIcon(name = "Filled.People") {
            materialPath {
                moveTo(16.0f, 11.0f)
                curveToRelative(1.66f, 0.0f, 2.99f, -1.34f, 2.99f, -3.0f)
                reflectiveCurveTo(17.66f, 5.0f, 16.0f, 5.0f)
                curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
                reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                close()
                moveTo(8.0f, 11.0f)
                curveToRelative(1.66f, 0.0f, 2.99f, -1.34f, 2.99f, -3.0f)
                reflectiveCurveTo(9.66f, 5.0f, 8.0f, 5.0f)
                curveTo(6.34f, 5.0f, 5.0f, 6.34f, 5.0f, 8.0f)
                reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                close()
                moveTo(8.0f, 13.0f)
                curveToRelative(-2.33f, 0.0f, -7.0f, 1.17f, -7.0f, 3.5f)
                verticalLineTo(19.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(-2.5f)
                curveToRelative(0.0f, -2.33f, -4.67f, -3.5f, -7.0f, -3.5f)
                close()
                moveTo(16.0f, 13.0f)
                curveToRelative(-0.29f, 0.0f, -0.62f, 0.02f, -0.97f, 0.05f)
                curveToRelative(1.16f, 0.84f, 1.97f, 1.97f, 1.97f, 3.45f)
                verticalLineTo(19.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(-2.5f)
                curveToRelative(0.0f, -2.33f, -4.67f, -3.5f, -7.0f, -3.5f)
                close()
            }
        }
    }

    val PermDeviceInformation: ImageVector by lazy {
        materialIcon(name = "Filled.PermDeviceInformation") {
            materialPath {
                moveTo(13.0f, 7.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(7.0f)
                close()
                moveTo(13.0f, 11.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(11.0f)
                close()
                moveTo(17.0f, 1.01f)
                lineTo(7.0f, 1.0f)
                curveTo(5.9f, 1.0f, 5.0f, 1.9f, 5.0f, 3.0f)
                verticalLineToRelative(18.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(10.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(3.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -1.99f, -2.0f, -1.99f)
                close()
                moveTo(17.0f, 19.0f)
                horizontalLineTo(7.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(10.0f)
                verticalLineTo(19.0f)
                close()
            }
        }
    }

    val PhoneInTalk: ImageVector by lazy {
        materialIcon(name = "Filled.PhoneInTalk") {
            materialPath {
                moveTo(20.0f, 15.5f)
                curveToRelative(-1.25f, 0.0f, -2.45f, -0.2f, -3.57f, -0.57f)
                curveToRelative(-0.35f, -0.11f, -0.74f, -0.03f, -1.02f, 0.24f)
                lineToRelative(-2.2f, 2.2f)
                curveToRelative(-2.83f, -1.44f, -5.15f, -3.75f, -6.59f, -6.59f)
                lineToRelative(2.2f, -2.21f)
                curveToRelative(0.28f, -0.26f, 0.36f, -0.65f, 0.25f, -1.0f)
                curveTo(8.7f, 6.45f, 8.5f, 5.25f, 8.5f, 4.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                horizontalLineTo(4.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                curveToRelative(0.0f, 9.39f, 7.61f, 17.0f, 17.0f, 17.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-3.5f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                close()
                moveTo(19.0f, 12.0f)
                horizontalLineToRelative(2.0f)
                curveToRelative(0.0f, -4.97f, -4.03f, -9.0f, -9.0f, -9.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(3.87f, 0.0f, 7.0f, 3.13f, 7.0f, 7.0f)
                close()
                moveTo(15.0f, 12.0f)
                horizontalLineToRelative(2.0f)
                curveToRelative(0.0f, -2.76f, -2.24f, -5.0f, -5.0f, -5.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
                close()
            }
        }
    }

    val Schedule: ImageVector by lazy {
        materialIcon(name = "Filled.Schedule") {
            materialPath {
                moveTo(11.99f, 2.0f)
                curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
                curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
                close()
                moveTo(12.0f, 20.0f)
                curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
                reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
                reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
                reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f)
                close()
            }
            materialPath {
                moveTo(12.5f, 7.0f)
                horizontalLineTo(11.0f)
                verticalLineToRelative(6.0f)
                lineToRelative(5.25f, 3.15f)
                lineToRelative(0.75f, -1.23f)
                lineToRelative(-4.5f, -2.67f)
                close()
            }
        }
    }

    val VideoCall: ImageVector by lazy {
        materialIcon(name = "Filled.VideoCall") {
            materialPath {
                moveTo(17.0f, 10.5f)
                verticalLineTo(7.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                horizontalLineTo(4.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(10.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(12.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-3.5f)
                lineToRelative(4.0f, 4.0f)
                verticalLineToRelative(-11.0f)
                lineToRelative(-4.0f, 4.0f)
                close()
                moveTo(14.0f, 13.0f)
                horizontalLineToRelative(-3.0f)
                verticalLineToRelative(3.0f)
                horizontalLineTo(9.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineTo(6.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(3.0f)
                verticalLineTo(8.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(3.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(2.0f)
                close()
            }
        }
    }

    val VolumeUp: ImageVector by lazy {
        materialIcon(name = "Filled.VolumeUp") {
            materialPath {
                moveTo(3.0f, 9.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(4.0f)
                lineToRelative(5.0f, 5.0f)
                verticalLineTo(4.0f)
                lineTo(7.0f, 9.0f)
                horizontalLineTo(3.0f)
                close()
                moveTo(16.5f, 12.0f)
                curveToRelative(0.0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
                verticalLineToRelative(8.05f)
                curveToRelative(1.48f, -0.73f, 2.5f, -2.25f, 2.5f, -4.02f)
                close()
                moveTo(14.0f, 3.23f)
                verticalLineToRelative(2.06f)
                curveToRelative(2.89f, 0.86f, 5.0f, 3.54f, 5.0f, 6.71f)
                reflectiveCurveToRelative(-2.11f, 5.85f, -5.0f, 6.71f)
                verticalLineToRelative(2.06f)
                curveToRelative(4.01f, -0.91f, 7.0f, -4.49f, 7.0f, -8.77f)
                reflectiveCurveToRelative(-2.99f, -7.86f, -7.0f, -8.77f)
                close()
            }
        }
    }

    val WbSunny: ImageVector by lazy {
        materialIcon(name = "Filled.WbSunny") {
            materialPath {
                moveTo(6.76f, 4.84f)
                lineToRelative(-1.8f, -1.79f)
                lineToRelative(-1.41f, 1.41f)
                lineToRelative(1.79f, 1.79f)
                lineToRelative(1.42f, -1.41f)
                close()
                moveTo(4.0f, 10.5f)
                horizontalLineTo(1.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(-2.0f)
                close()
                moveTo(13.0f, 0.55f)
                horizontalLineToRelative(-2.0f)
                verticalLineTo(3.5f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(0.55f)
                close()
                moveTo(20.45f, 4.46f)
                lineToRelative(-1.41f, -1.41f)
                lineToRelative(-1.79f, 1.79f)
                lineToRelative(1.41f, 1.41f)
                lineToRelative(1.79f, -1.79f)
                close()
                moveTo(17.24f, 18.16f)
                lineToRelative(1.79f, 1.8f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(-1.8f, -1.79f)
                lineToRelative(-1.4f, 1.4f)
                close()
                moveTo(20.0f, 10.5f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-3.0f)
                close()
                moveTo(12.0f, 5.5f)
                curveToRelative(-3.31f, 0.0f, -6.0f, 2.69f, -6.0f, 6.0f)
                reflectiveCurveToRelative(2.69f, 6.0f, 6.0f, 6.0f)
                reflectiveCurveToRelative(6.0f, -2.69f, 6.0f, -6.0f)
                reflectiveCurveToRelative(-2.69f, -6.0f, -6.0f, -6.0f)
                close()
                moveTo(11.0f, 22.45f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(19.5f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.95f)
                close()
                moveTo(3.55f, 18.54f)
                lineToRelative(1.41f, 1.41f)
                lineToRelative(1.79f, -1.8f)
                lineToRelative(-1.41f, -1.41f)
                lineToRelative(-1.79f, 1.8f)
                close()
            }
        }
    }

    val Mic: ImageVector by lazy {
        materialIcon(name = "Filled.Mic") {
            materialPath {
                moveTo(12f, 14f)
                curveTo(13.66f, 14f, 14.99f, 12.66f, 14.99f, 11f)
                lineTo(15f, 5f)
                curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
                reflectiveCurveTo(9f, 3.34f, 9f, 5f)
                verticalLineTo(11f)
                curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
                close()
                moveTo(17.3f, 11f)
                curveTo(17.3f, 14f, 14.76f, 16.1f, 12f, 16.1f)
                reflectiveCurveTo(6.7f, 14f, 6.7f, 11f)
                horizontalLineTo(5f)
                curveTo(5f, 14.41f, 7.72f, 17.23f, 11f, 17.72f)
                verticalLineTo(21f)
                horizontalLineTo(13f)
                verticalLineTo(17.72f)
                curveTo(16.28f, 17.24f, 19f, 14.42f, 19f, 11f)
                horizontalLineTo(17.3f)
                close()
            }
        }
    }

    val Flashlight: ImageVector by lazy {
        materialIcon(name = "Filled.FlashlightCustom") {
            addPath(
                pathData = PathParser().parsePathString(
                    "M6,2h12v6l-2,2v10c0,1.1-0.9,2-2,2h-4c-1.1,0-2-0.9-2-2V10L6,8V2z " +
                        "M8,4v3h8V4H8z M10,10v2h4v-2H10z"
                ).toNodes(),
                fill = SolidColor(Color.Black)
            )
        }
    }

    val Search: ImageVector by lazy {
        materialIcon(name = "Filled.SearchCustom") {
            addPath(
                pathData = PathParser().parsePathString(
                    "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59,16,11.11,16,9.5 " +
                        "C16,5.91,13.09,3,9.5,3S3,5.91,3,9.5S5.91,16,9.5,16 " +
                        "c1.61,0,3.09,-0.59,4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19L15.5,14z " +
                        "M9.5,14C7.01,14,5,11.99,5,9.5S7.01,5,9.5,5S14,7.01,14,9.5S11.99,14,9.5,14z"
                ).toNodes(),
                fill = SolidColor(Color.Black)
            )
        }
    }

    val Calculate: ImageVector by lazy {
        materialIcon(name = "Filled.CalculateCustom") {
            addPath(
                pathData = PathParser().parsePathString(
                    "M19,3H5C3.9,3,3,3.9,3,5v14c0,1.1,0.9,2,2,2h14c1.1,0,2,-0.9,2,-2V5 " +
                        "C21,3.9,20.1,3,19,3z " +
                        "M13.03,7.06L14.09,6l1.41,1.41L16.91,6l1.06,1.06l-1.41,1.41 " +
                        "l1.41,1.41l-1.06,1.06L15.5,9.54l-1.41,1.41l-1.06,-1.06l1.41,-1.41L13.03,7.06z " +
                        "M6.25,7.72h5v1.5h-5V7.72z M6.25,12.48h5v1.5h-5V12.48z " +
                        "M6.25,17.24h5v1.5h-5V17.24z " +
                        "M13.03,16.94l-1.41,-1.41l1.06,-1.06l1.41,1.41l1.41,-1.41l1.06,1.06 " +
                        "l-1.41,1.41l1.41,1.41l-1.06,1.06L15.5,17.99l-1.41,1.41l-1.06,-1.06L13.03,16.94z"
                ).toNodes(),
                fill = SolidColor(Color.Black)
            )
        }
    }

}
