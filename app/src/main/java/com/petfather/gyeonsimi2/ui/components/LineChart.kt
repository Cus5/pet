package com.petfather.gyeonsimi2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class ChartPoint(val date: LocalDate, val value: Double)

private val DayFormat = DateTimeFormatter.ofPattern("M/d")

/**
 * 날짜별 값을 부드러운 곡선 + 면적으로 보여주는 차트.
 * [normalRange] 를 주면 정상 범위를 옅은 띠로 표시합니다.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    normalRange: ClosedFloatingPointRange<Double>? = null,
    unit: String = "",
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = colors.onSurfaceVariant)
    val description = points.joinToString { "${it.date.format(DayFormat)} ${it.value}$unit" }

    Canvas(modifier.fillMaxWidth().height(height).semantics { contentDescription = "그래프: $description" }) {
        if (points.isEmpty()) return@Canvas
        val values = points.map { it.value } + listOfNotNull(normalRange?.start, normalRange?.endInclusive)
        var minV = values.min()
        var maxV = values.max()
        if (maxV - minV < 0.5) { minV -= 0.5; maxV += 0.5 }
        val pad = (maxV - minV) * 0.12
        minV -= pad; maxV += pad

        val left = 34.dp.toPx()
        val bottom = size.height - 20.dp.toPx()
        val top = 6.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val w = right - left
        val h = bottom - top
        fun x(i: Int) = if (points.size == 1) left + w / 2 else left + w * i / (points.size - 1)
        fun y(v: Double) = (bottom - h * ((v - minV) / (maxV - minV))).toFloat()

        // 가로 눈금선 + y축 숫자
        val ticks = 4
        for (t in 0..ticks) {
            val v = minV + (maxV - minV) * t / ticks
            val yy = y(v)
            drawLine(colors.outlineVariant.copy(alpha = 0.6f), Offset(left, yy), Offset(right, yy), strokeWidth = 1f)
            val text = if (maxV - minV >= 10) "%.0f".format(java.util.Locale.US, v) else "%.1f".format(java.util.Locale.US, v)
            val layout = measurer.measure(text, labelStyle)
            drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), yy - layout.size.height / 2))
        }

        normalRange?.let { r ->
            val y1 = y(r.endInclusive)
            val y2 = y(r.start)
            drawRect(colors.primary.copy(alpha = 0.07f), topLeft = Offset(left, y1), size = Size(w, y2 - y1))
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            drawLine(colors.primary.copy(alpha = 0.35f), Offset(left, y1), Offset(right, y1), 1.5f, pathEffect = dash)
            drawLine(colors.primary.copy(alpha = 0.35f), Offset(left, y2), Offset(right, y2), 1.5f, pathEffect = dash)
        }

        val pts = points.mapIndexed { i, p -> Offset(x(i), y(p.value)) }
        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val p0 = pts[i - 1]
                val p1 = pts[i]
                val cx = (p0.x + p1.x) / 2
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }
        if (pts.size > 1) {
            val area = Path().apply {
                addPath(line)
                lineTo(pts.last().x, bottom)
                lineTo(pts.first().x, bottom)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.22f), colors.primary.copy(alpha = 0.02f)), startY = top, endY = bottom))
            drawPath(line, colors.primary, style = Stroke(width = 2.5.dp.toPx()))
        }
        pts.forEach { p ->
            drawCircle(colors.surfaceContainerLow, radius = 5.dp.toPx(), center = p)
            drawCircle(colors.primary, radius = 3.5.dp.toPx(), center = p)
        }

        // x축 날짜: 처음과 끝을 포함해 고르게 최대 6개만 표시
        labelIndices(points.size, 6).forEach { i ->
            val layout = measurer.measure(points[i].date.format(DayFormat), labelStyle)
            val lx = (x(i) - layout.size.width / 2).coerceIn(left - 6.dp.toPx(), size.width - layout.size.width.toFloat())
            drawText(layout, topLeft = Offset(lx, bottom + 5.dp.toPx()))
        }
    }
}

/** 0..count-1 중 처음과 끝을 포함해 고르게 고른 최대 [max]개의 위치 */
fun labelIndices(count: Int, max: Int): List<Int> = when {
    count <= 0 -> emptyList()
    count <= max -> (0 until count).toList()
    else -> (0 until max).map { k -> (k * (count - 1).toDouble() / (max - 1)).roundToInt() }.distinct()
}
