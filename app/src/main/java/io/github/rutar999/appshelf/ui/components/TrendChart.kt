package io.github.rutar999.appshelf.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 折れ線グラフ（F-16 長期トレンド用）。
 *
 * グラフライブラリは入れていない。折れ線と面の塗りだけなら Canvas で十分で、
 * 「依存を最小限に保つ」という本プロジェクトの方針とも一致する。
 *
 * @param values 時系列の値。左が古く右が新しい。空や 1 件のときは呼び出し側で出し分けること
 * @param startLabel 左端（最古）の軸ラベル
 * @param endLabel 右端（最新）の軸ラベル
 * @param maxLabel 最大値のラベル（単位つきの整形済み文字列）
 */
@Composable
fun TrendChart(
    values: List<Float>,
    startLabel: String,
    endLabel: String,
    maxLabel: String,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    height: Int = 120
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val fillColor = lineColor.copy(alpha = 0.18f)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = maxLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            if (values.isEmpty()) return@Canvas

            // 0 を底とする。最大値が 0 のときは平坦な線を底に引く
            val maxValue = values.max().takeIf { it > 0f } ?: 1f
            val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f

            fun pointAt(index: Int): Offset {
                val x = if (values.size > 1) stepX * index else size.width / 2f
                val y = size.height - (values[index] / maxValue) * size.height
                return Offset(x, y)
            }

            // 上端・中央・下端の目安線
            listOf(0f, 0.5f, 1f).forEach { ratio ->
                val y = size.height * ratio
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            if (values.size == 1) {
                drawCircle(color = lineColor, radius = 5f, center = pointAt(0))
                return@Canvas
            }

            val linePath = Path().apply {
                moveTo(pointAt(0).x, pointAt(0).y)
                for (i in 1 until values.size) lineTo(pointAt(i).x, pointAt(i).y)
            }
            // 折れ線の下を薄く塗って推移を見やすくする
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path = fillPath, color = fillColor)
            drawPath(path = linePath, color = lineColor, style = Stroke(width = 3f))

            values.indices.forEach { drawCircle(color = lineColor, radius = 3.5f, center = pointAt(it)) }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
