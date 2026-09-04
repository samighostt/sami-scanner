package com.sami.auditor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sami.auditor.R
import com.sami.auditor.data.model.LogSeverity
import com.sami.auditor.ui.theme.SamiBackground
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

@Composable
fun AppHeader(
    hasReport: Boolean,
    onOpenExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SamiCardBg)
                    .border(1.5.dp, SamiGold, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_sami_cyber),
                    contentDescription = "SAMI Security Emblem",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "S A M I",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = SamiGold
                )
                Text(
                    text = "ADVANCED CYBER AUDITOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = SamiMuted
                )
            }
        }

        if (hasReport) {
            OutlinedButton(
                onClick = onOpenExport,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SamiGold),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SamiGold)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_export_header")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export Report",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("تصدير التقرير", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UrlInputField(
    url: String,
    onUrlChange: (String) -> Unit,
    onClear: () -> Unit,
    onScan: () -> Unit,
    isScanning: Boolean
) {
    OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
        placeholder = {
            Text(
                text = stringResource(R.string.url_hint),
                color = SamiMuted,
                fontSize = 13.sp
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(onGo = { onScan() }),
        trailingIcon = {
            if (url.isNotEmpty() && !isScanning) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = SamiMuted
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SamiText,
            unfocusedTextColor = SamiText,
            focusedBorderColor = SamiGold,
            unfocusedBorderColor = SamiCardBorder,
            focusedContainerColor = SamiCardBg,
            unfocusedContainerColor = SamiCardBg,
            cursorColor = SamiGold
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("url_input_field")
    )
}

@Composable
fun SampleUrlChips(
    onSelectUrl: (String) -> Unit,
    enabled: Boolean
) {
    val samples = listOf(
        "https://owasp.org",
        "https://google.com",
        "https://github.com",
        "http://neverssl.com"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        samples.forEach { sample ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SamiCardBg)
                    .border(1.dp, SamiCardBorder, RoundedCornerShape(16.dp))
                    .clickable(enabled = enabled) { onSelectUrl(sample) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = sample,
                    color = if (enabled) SamiMuted else SamiMuted.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ActionControlsRow(
    isScanning: Boolean,
    hasResults: Boolean,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onOpenExport: () -> Unit,
    onCopyReport: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isScanning) {
            Button(
                onClick = onCancelScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SamiBad,
                    contentColor = SamiText
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_cancel_audit")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إلغاء الفحص",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        } else {
            Button(
                onClick = onStartScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SamiGold,
                    contentColor = SamiBackground
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_start_audit")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "بدء الفحص الأمني",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }

        if (hasResults && !isScanning) {
            OutlinedButton(
                onClick = onOpenExport,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SamiGold),
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SamiGold)
                ),
                modifier = Modifier.testTag("btn_open_export")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("تصدير", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = onCopyReport,
                modifier = Modifier
                    .background(SamiCardBg, RoundedCornerShape(8.dp))
                    .border(1.dp, SamiCardBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = SamiText
                )
            }

            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .background(SamiCardBg, RoundedCornerShape(8.dp))
                    .border(1.dp, SamiCardBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear",
                    tint = SamiMuted
                )
            }
        }
    }
}

@Composable
fun StatusBanner(
    statusText: String,
    severity: LogSeverity,
    isScanning: Boolean
) {
    val statusColor = when (severity) {
        LogSeverity.GOOD -> SamiGood
        LogSeverity.BAD -> SamiBad
        LogSeverity.WARN -> SamiWarn
        LogSeverity.HEADER -> SamiGold
        LogSeverity.INFO -> SamiInfo
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SamiCardBg)
            .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = SamiGold,
                    strokeWidth = 2.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = statusText,
                color = if (isScanning) SamiGold else SamiText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isScanning) pulseAlpha else 1f)
            )
        }
    }
}
