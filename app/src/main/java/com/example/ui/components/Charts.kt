package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

// Data class for Slices
data class ChartSlice(
    val label: String,
    val value: Double,
    val percentage: Float,
    val color: Color
)

fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    return format.format(amount).replace(",00", "").replace("Rp", "Rp ")
}

/**
 * 1. Donut Chart (Pengeluaran per Kategori)
 */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animateSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Donut Arc Drawing
        Box(
            modifier = Modifier
                .weight(1.2f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                val strokeWidth = 55f
                val size = size.minDimension - strokeWidth

                slices.forEach { slice ->
                    val sweepAngle = slice.percentage * 360f * animateSweep
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset((this.size.width - size) / 2, (this.size.height - size) / 2),
                        size = Size(size, size),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += slice.percentage * 360f
                }
            }

            // Central Summary Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pengeluaran",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                val totalExp = slices.sumOf { it.value }
                Text(
                    text = formatRupiah(totalExp).replace("Rp ", ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right side: Legend List
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            slices.take(6).forEach { slice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${String.format("%.1f", slice.percentage * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * 2. Clickable Multi-Line Chart (Tren per Kategori)
 */
@Composable
fun MultiLineChart(
    categoryData: Map<String, List<Double>>, // Category name -> 5 weekly values
    colors: Map<String, Color>,
    modifier: Modifier = Modifier
) {
    var activeCategories by remember { mutableStateOf(categoryData.keys.toSet()) }
    val weeks = listOf("Week 1", "Week 2", "Week 3", "Week 4", "Week 5")
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier.fillMaxWidth()) {
        // Toggle Buttons (Chips) as Legend Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categoryData.keys.forEach { cat ->
                val isActive = activeCategories.contains(cat)
                val color = colors[cat] ?: MintGreen
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isActive) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isActive) color else Color.LightGray),
                    modifier = Modifier
                        .clickable {
                            activeCategories = if (isActive) {
                                if (activeCategories.size > 1) activeCategories - cat else activeCategories
                            } else {
                                activeCategories + cat
                            }
                        }
                ) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Line Grid & Plotting
        val maxValue = remember(categoryData, activeCategories) {
            val maxVal = categoryData.filter { activeCategories.contains(it.key) }
                .values.flatten().maxOrNull() ?: 1000.0
            if (maxVal == 0.0) 1000.0 else maxVal
        }

        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val paddingLeft = 100f
                val paddingBottom = 60f
                val chartWidth = size.width - paddingLeft
                val chartHeight = size.height - paddingBottom

                // Draw horizontal grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = chartHeight - (chartHeight / gridLines) * i
                    val gridValue = (maxValue / gridLines) * i
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )

                    // Draw labels on Y axis
                    val formattedVal = if (gridValue >= 1000) "${(gridValue / 1000).toInt()}k" else "${gridValue.toInt()}"
                    drawText(
                        textMeasurer = textMeasurer,
                        text = formattedVal,
                        topLeft = Offset(10f, y - 18f),
                        style = TextStyle(fontSize = 10.sp, color = onSurfaceColor.copy(alpha = 0.6f))
                    )
                }

                // Draw X Labels
                val colWidth = chartWidth / 4f
                weeks.forEachIndexed { index, week ->
                    val x = paddingLeft + colWidth * index
                    drawText(
                        textMeasurer = textMeasurer,
                        text = week,
                        topLeft = Offset(x - 40f, chartHeight + 10f),
                        style = TextStyle(fontSize = 10.sp, color = onSurfaceColor.copy(alpha = 0.6f))
                    )
                }

                // Draw lines for active categories
                categoryData.filter { activeCategories.contains(it.key) }.forEach { (cat, vals) ->
                    val path = Path()
                    val color = colors[cat] ?: MintGreen

                    vals.forEachIndexed { index, value ->
                        val x = paddingLeft + colWidth * index
                        val y = chartHeight - ((value / maxValue).toFloat() * chartHeight)

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            // Draw nice curve or straight lines
                            path.lineTo(x, y)
                        }

                        // Draw point circle
                        drawCircle(
                            color = color,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }

                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

/**
 * 3. Cash Flow Trend Chart (Tren Arus Kas: Income & Expense Bars + Net Line)
 */
@Composable
fun CashFlowTrendChart(
    incomeData: List<Double>,   // 5 points
    expenseData: List<Double>,  // 5 points
    modifier: Modifier = Modifier
) {
    val weeks = listOf("Week 1", "Week 2", "Week 3", "Week 4", "Week 5")
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val maxVal = remember(incomeData, expenseData) {
        val maxInc = incomeData.maxOrNull() ?: 1000.0
        val maxExp = expenseData.maxOrNull() ?: 1000.0
        val maxCombined = maxOf(maxInc, maxExp)
        if (maxCombined == 0.0) 1000.0 else maxCombined
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).background(IncomeGreen, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pemasukan", style = MaterialTheme.typography.bodySmall, color = onSurfaceColor.copy(alpha = 0.8f))
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Box(modifier = Modifier.size(12.dp).background(ExpenseRed, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pengeluaran", style = MaterialTheme.typography.bodySmall, color = onSurfaceColor.copy(alpha = 0.8f))

            Spacer(modifier = Modifier.width(16.dp))
            
            Box(modifier = Modifier.size(12.dp).background(NetBlue, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Net Bersih", style = MaterialTheme.typography.bodySmall, color = onSurfaceColor.copy(alpha = 0.8f))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val paddingLeft = 100f
                val paddingBottom = 60f
                val chartWidth = size.width - paddingLeft
                val chartHeight = size.height - paddingBottom

                // Draw horizontal grids & scale
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = chartHeight - (chartHeight / gridLines) * i
                    val gridValue = (maxVal / gridLines) * i
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )

                    val formattedVal = if (gridValue >= 1000) "${(gridValue / 1000).toInt()}k" else "${gridValue.toInt()}"
                    drawText(
                        textMeasurer = textMeasurer,
                        text = formattedVal,
                        topLeft = Offset(10f, y - 18f),
                        style = TextStyle(fontSize = 10.sp, color = onSurfaceColor.copy(alpha = 0.6f))
                    )
                }

                // Bar & Line computations
                val colWidth = chartWidth / 5f
                val barWidth = 16f
                val spacing = 4f

                val netPoints = ArrayList<Offset>()

                for (i in 0..4) {
                    val centerX = paddingLeft + colWidth * i + colWidth / 2f

                    // 1. Draw Pemasukan (Income) Bar
                    val incVal = incomeData.getOrElse(i) { 0.0 }
                    val incHeight = (incVal / maxVal).toFloat() * chartHeight
                    val incX = centerX - barWidth - spacing
                    val incY = chartHeight - incHeight
                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(incX, incY),
                        size = Size(barWidth, incHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // 2. Draw Pengeluaran (Expense) Bar
                    val expVal = expenseData.getOrElse(i) { 0.0 }
                    val expHeight = (expVal / maxVal).toFloat() * chartHeight
                    val expX = centerX + spacing
                    val expY = chartHeight - expHeight
                    drawRoundRect(
                        color = ExpenseRed,
                        topLeft = Offset(expX, expY),
                        size = Size(barWidth, expHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // 3. Compute Net Bersih (Net Income) Node
                    val netVal = incVal - expVal
                    val netHeight = (netVal.coerceAtLeast(0.0) / maxVal).toFloat() * chartHeight
                    val netY = chartHeight - netHeight
                    netPoints.add(Offset(centerX, netY))

                    // Draw X Label
                    drawText(
                        textMeasurer = textMeasurer,
                        text = weeks[i],
                        topLeft = Offset(centerX - 40f, chartHeight + 10f),
                        style = TextStyle(fontSize = 10.sp, color = onSurfaceColor.copy(alpha = 0.6f))
                    )
                }

                // 4. Plot Net Bersih Line
                if (netPoints.isNotEmpty()) {
                    val linePath = Path()
                    netPoints.forEachIndexed { index, pt ->
                        if (index == 0) linePath.moveTo(pt.x, pt.y)
                        else linePath.lineTo(pt.x, pt.y)
                        drawCircle(color = NetBlue, radius = 6f, center = pt)
                    }
                    drawPath(
                        path = linePath,
                        color = NetBlue,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

/**
 * 4. Sankey Flow (Aliran Dana: Income -> Balance -> Expenses)
 * Draws a flowing energy stream graphic using Canvas curves.
 */
@Composable
fun SankeyFlowChart(
    incomeSource: String,
    incomeAmount: Double,
    topExpenseCategory: String,
    topExpenseAmount: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Coordinates for Income (Left Box)
            val leftBoxX = 10f
            val leftBoxY = height / 4f
            val leftBoxH = height / 2f
            val leftBoxW = 140f

            // Coordinates for Expense (Right Box)
            val rightBoxX = width - 150f
            val rightBoxY = height / 4f
            val rightBoxH = height / 2f
            val rightBoxW = 140f

            // Create flowing Bezier Path
            val flowPath = Path()
            flowPath.moveTo(leftBoxX + leftBoxW, leftBoxY)
            
            // Curve from top-right of left box to top-left of right box
            flowPath.cubicTo(
                (leftBoxX + leftBoxW + rightBoxX) / 2f, leftBoxY,
                (leftBoxX + leftBoxW + rightBoxX) / 2f, rightBoxY,
                rightBoxX, rightBoxY
            )
            flowPath.lineTo(rightBoxX, rightBoxY + rightBoxH)
            
            // Curve from bottom-left of right box to bottom-right of left box
            flowPath.cubicTo(
                (leftBoxX + leftBoxW + rightBoxX) / 2f, rightBoxY + rightBoxH,
                (leftBoxX + leftBoxW + rightBoxX) / 2f, leftBoxY + leftBoxH,
                leftBoxX + leftBoxW, leftBoxY + leftBoxH
            )
            flowPath.close()

            // Draw flowing ribbon using custom Gradient Brush
            val gradientBrush = Brush.horizontalGradient(
                colors = listOf(IncomeGreen.copy(alpha = 0.5f), ExpenseRed.copy(alpha = 0.5f)),
                startX = leftBoxX + leftBoxW,
                endX = rightBoxX
            )

            drawPath(path = flowPath, brush = gradientBrush)

            // Draw boundaries/accents
            drawLine(
                color = IncomeGreen,
                start = Offset(leftBoxX + leftBoxW, leftBoxY),
                end = Offset(leftBoxX + leftBoxW, leftBoxY + leftBoxH),
                strokeWidth = 6f
            )
            drawLine(
                color = ExpenseRed,
                start = Offset(rightBoxX, rightBoxY),
                end = Offset(rightBoxX, rightBoxY + rightBoxH),
                strokeWidth = 6f
            )
        }

        // Overlay text labels on left & right
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(90.dp)
                    .padding(start = 4.dp)
            ) {
                Text(
                    text = incomeSource,
                    style = MaterialTheme.typography.titleSmall,
                    color = IncomeGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "PEMASUKAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = formatRupiah(incomeAmount).replace("Rp ", "Rp\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier
                    .width(90.dp)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = topExpenseCategory,
                    style = MaterialTheme.typography.titleSmall,
                    color = ExpenseRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "PENGELUARAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = formatRupiah(topExpenseAmount).replace("Rp ", "Rp\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
