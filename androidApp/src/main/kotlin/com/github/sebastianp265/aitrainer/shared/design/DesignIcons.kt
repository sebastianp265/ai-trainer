package com.github.sebastianp265.aitrainer.shared.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal object DesignIcons {
    val Training = lineIcon("training", "M6 7v10M18 7v10M3 10v4M21 10v4M6 12h12")
    val Plan = lineIcon("plan", "M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-11a2 2 0 0 1 2 -2z", "M4 10h16M9 3v4M15 3v4")
    val Coach = lineIcon("coach", "M5 5h14a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1h-9l-4 3v-3H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z")
    val Progress = lineIcon("progress", "M4 19h16M7 16v-5M12 16V7M17 16v-8")
    val Settings = lineIcon(
        "settings",
        circle(12f, 12f, 3f),
        "M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M5.6 18.4l2.1-2.1M16.3 7.7l2.1-2.1",
    )
    val Back = lineIcon("back", "M19 12H5M11 6l-6 6 6 6")
    val Close = lineIcon("close", "M6 6l12 12M18 6L6 18")
    val Add = lineIcon("add", "M12 5v14M5 12h14")
    val Play = lineIcon("play", "M7 5v14l12-7z")
    val Delete = lineIcon("delete", "M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13")
    val Check = lineIcon("check", "M5 12l5 5 9-10")
    val Edit = lineIcon("edit", "M4 20h4L19 9l-4-4L4 16v4zM13 7l4 4")
    val Send = lineIcon("send", "M4 12l16-8-6 16-3-7-7-1z")
    val ChevronRight = lineIcon("chevron_right", "M9 6l6 6-6 6")
    val Warning = lineIcon("warning", "M12 4l9 16H3l9-16zM12 10v4M12 17v.5")
    val Key = lineIcon("key", circle(8f, 15f, 4f), "M11 12l9-9M17 6l3 3M14 9l2 2")
    val Search = lineIcon("search", circle(11f, 11f, 6f), "M16 16l4 4")
    val Info = lineIcon("info", circle(12f, 12f, 9f), "M12 11v6M12 7.5v.5")
}

private const val STROKE_WIDTH = 1.8f
private const val VIEWPORT = 24f

private fun lineIcon(name: String, vararg paths: String): ImageVector =
    paths.fold(ImageVector.Builder(name, 24.dp, 24.dp, VIEWPORT, VIEWPORT)) { builder, path ->
        builder.addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 -${2 * r} 0z"
