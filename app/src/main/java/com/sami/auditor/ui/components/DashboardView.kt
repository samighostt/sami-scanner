package com.sami.auditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sami.auditor.data.model.AuditHistoryItem
import com.sami.auditor.ui.theme.SamiBad
import com.sami.auditor.ui.theme.SamiCardBg
import com.sami.auditor.ui.theme.SamiCardBorder
import com.sami.auditor.ui.theme.SamiCardElevated
import com.sami.auditor.ui.theme.SamiGold
import com.sami.auditor.ui.theme.SamiGood
import com.sami.auditor.ui.theme.SamiInfo
import com.sami.auditor.ui.theme.SamiMuted
import com.sami.auditor.ui.theme.SamiText
import com.sami.auditor.ui.theme.SamiWarn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardView(
    history: List<AuditHistoryItem>,
    onReScan: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val totalScans = history.size
    val averageScore = if (history.isNotEmpty()) history.map { it.score }.average().toInt() else 0
    val totalCritical = history.sumOf { it.criticalCount }
    val totalWarnings = history.sumOf { it.warningCount }
    val totalPassed = history.sumOf { it.passedCount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Executive Summary Metrics
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SamiCardBg)
                    .border(1.dp, SamiCardBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = SamiGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "لوحة المؤشرات والإحصائيات العامة",
                            color = SamiGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBox(
                            title = "الفحوصات",
                            value = "$totalScans",
                            color = SamiGold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MetricBox(
                            title = "متوسط الأمان",
                            value = "$averageScore%",
                            color = if (averageScore >= 70) SamiGood else SamiWarn,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MetricBox(
                            title = "ثغرات حرجة",
                            value = "$totalCritical",
                            color = if (totalCritical > 0) SamiBad else SamiGood,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary breakdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SamiCardElevated)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(
                            text = "تحذيرات: $totalWarnings",
                            color = SamiWarn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "فحوصات ناجحة: $totalPassed",
                            color = SamiGood,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = SamiGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سجل المواقع المفحوصة سابقاً (${history.size})",
                        color = SamiText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (history.isNotEmpty()) {
                    IconButton(onClick = onClearHistory) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = SamiMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد فحوصات سابقة بعد. قم بإجراء أول فحص لحفظه هنا.",
                        color = SamiMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(history, key = { it.id }) { item ->
                HistoryItemCard(item = item, onReScan = { onReScan(item.url) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SamiCardElevated)
            .border(1.dp, SamiCardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = title,
                color = SamiMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: AuditHistoryItem,
    onReScan: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(Date(item.timestamp))
    val scoreColor = when {
        item.score >= 80 -> SamiGood
        item.score >= 50 -> SamiWarn
        else -> SamiBad
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SamiCardBg)
            .border(1.dp, SamiCardBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.url,
                    color = SamiText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = formattedDate,
                    color = SamiMuted,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.criticalCount > 0) {
                        Text(
                            text = "${item.criticalCount} حرج  •  ",
                            color = SamiBad,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${item.warningCount} تحذير  •  ${item.passedCount} آمن",
                        color = SamiMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(scoreColor.copy(alpha = 0.2f))
                        .border(1.dp, scoreColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${item.score}%",
                        color = scoreColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onReScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SamiGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "إعادة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
