package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ChartColors = listOf(
    Color(0xFF00F2FE), // GlowBlue
    Color(0xFFF15BB5), // GlowPink
    Color(0xFF9D4EDD), // GlowPurple
    Color(0xFF00FF87), // Neon Green
    Color(0xFFFFD700), // Gold
    Color(0xFFFF3366)  // Neon Red
)

@Composable
fun SimpleBarChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF00F2FE)
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { Text("لا توجد بيانات", color = Color.White) }
        return
    }
    
    val maxVal = data.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val entries = data.entries.toList()
    
    Canvas(modifier = modifier.padding(16.dp)) {
        val barWidth = size.width / (entries.size * 2f)
        val space = barWidth
        
        entries.forEachIndexed { index, entry ->
            val barHeight = (entry.value / maxVal).toFloat() * size.height
            val x = index * (barWidth + space) + space / 2f
            val y = size.height - barHeight
            
            drawRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun SimpleLineChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFFF15BB5)
) {
    if (data.isEmpty() || data.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { Text("لا توجد بيانات كافية", color = Color.White) }
        return
    }
    
    val maxVal = data.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val minVal = data.values.minOrNull() ?: 0.0
    val range = (maxVal - minVal).takeIf { it > 0 } ?: 1.0
    val entries = data.entries.toList()
    
    Canvas(modifier = modifier.padding(16.dp)) {
        val stepX = size.width / (entries.size - 1).coerceAtLeast(1)
        val path = Path()
        
        entries.forEachIndexed { index, entry ->
            val normalizedY = ((entry.value - minVal) / range).toFloat()
            val x = index * stepX
            val y = size.height - (normalizedY * size.height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
        }
        
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f)
        )
    }
}

@Composable
fun SimplePieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    colors: List<Color> = ChartColors,
    textColor: Color = Color.White
) {
    val total = data.values.sum()
    if (total <= 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { Text("لا توجد بيانات", color = textColor) }
        return
    }
    
    val entries = data.entries.toList()
    
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(150.dp).padding(16.dp)) {
            var startAngle = -90f
            entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / total).toFloat() * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(colors[index % colors.size]))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(entry.key, fontSize = 12.sp, color = textColor)
                }
            }
        }
    }
}
