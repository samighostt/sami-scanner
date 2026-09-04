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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sami.auditor.data.model.MonitoredSite
import com.sami.auditor.ui.theme.SamiBad
import com.sami.auditor.ui.theme.SamiCardBg
import com.sami.auditor.ui.theme.SamiCardBorder
import com.sami.auditor.ui.theme.SamiGold
import com.sami.auditor.ui.theme.SamiGood
import com.sami.auditor.ui.theme.SamiMuted
import com.sami.auditor.ui.theme.SamiText
import com.sami.auditor.ui.theme.SamiWarn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonitoringView(
    monitoredSites: List<MonitoredSite>,
    onAddSite: (url: String, intervalHours: Int) -> Unit,
    onRemoveSite: (id: String) -> Unit,
    onCheckSiteNow: (MonitoredSite) -> Unit
) {
    var newUrlInput by remember { mutableStateOf("") }
    var selectedIntervalHours by remember { mutableStateOf(24) } // 24 = Daily, 168 = Weekly

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Add Site Card
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
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SamiGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جدولة المراقبة الدورية (Continuous Monitoring)",
                            color = SamiGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "أضف نطاقاتك الهامة ليتم تتبع تغيراتها الأمنية وشهادة SSL وفحص ظهور ثغرات جديدة بشكل دوري.",
                        color = SamiMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newUrlInput,
                        onValueChange = { newUrlInput = it },
                        placeholder = { Text("https://my-site.com", color = SamiMuted, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SamiText,
                            unfocusedTextColor = SamiText,
                            focusedBorderColor = SamiGold,
                            unfocusedBorderColor = SamiCardBorder,
                            focusedContainerColor = SamiCardBg,
                            unfocusedContainerColor = SamiCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IntervalOptionChip(
                                label = "يومياً (24h)",
                                isSelected = selectedIntervalHours == 24,
                                onClick = { selectedIntervalHours = 24 }
                            )
                            IntervalOptionChip(
                                label = "أسبوعياً (7d)",
                                isSelected = selectedIntervalHours == 168,
                                onClick = { selectedIntervalHours = 168 }
                            )
                        }

                        Button(
                            onClick = {
                                if (newUrlInput.isNotBlank()) {
                                    onAddSite(newUrlInput.trim(), selectedIntervalHours)
                                    newUrlInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SamiGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة للمراقبة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Monitored Sites List
        item {
            Text(
                text = "المواقع المجدولة للمراقبة (${monitoredSites.size})",
                color = SamiText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (monitoredSites.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مواقع مضافة في جدول المراقبة حالياً.",
                        color = SamiMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(monitoredSites, key = { it.id }) { site ->
                MonitoredSiteCard(
                    site = site,
                    onCheckNow = { onCheckSiteNow(site) },
                    onDelete = { onRemoveSite(site.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IntervalOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SamiGold.copy(alpha = 0.2f) else SamiCardBg)
            .border(
                1.dp,
                if (isSelected) SamiGold else SamiCardBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) SamiGold else SamiMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun MonitoredSiteCard(
    site: MonitoredSite,
    onCheckNow: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (site.status) {
        "SECURE" -> SamiGood
        "WARNING" -> SamiWarn
        "CRITICAL" -> SamiBad
        else -> SamiGold
    }

    val lastCheckedText = if (site.lastChecked > 0) {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        "آخر فحص: ${sdf.format(Date(site.lastChecked))}"
    } else {
        "بانتظار أول فحص"
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
                    text = site.url,
                    color = SamiText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${if (site.intervalHours == 24) "دورية يومية (24h)" else "دورية أسبوعية (7d)"} • $lastCheckedText",
                    color = SamiMuted,
                    fontSize = 10.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (site.lastScore > 0) "${site.lastScore}%" else site.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = onCheckNow, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Scan Now",
                        tint = SamiGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = SamiMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
