package com.sami.auditor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sami.auditor.R
import com.sami.auditor.data.model.AuditLogEntry
import com.sami.auditor.data.model.AuditReport
import com.sami.auditor.data.model.CookieFinding
import com.sami.auditor.data.model.LogSeverity
import com.sami.auditor.data.model.PathProbeResult
import com.sami.auditor.data.model.ScanStatus
import com.sami.auditor.data.model.SecurityHeaderItem
import com.sami.auditor.data.model.TechCorsFinding
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
fun AuditScreen(
    viewModel: AuditViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SamiBackground),
        containerColor = SamiBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SamiBackground)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.footer_notice),
                    color = SamiMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            AppHeader()

            Spacer(modifier = Modifier.height(12.dp))

            // URL Input Field
            UrlInputField(
                url = uiState.urlInput,
                onUrlChange = { viewModel.onUrlChanged(it) },
                onClear = { viewModel.onUrlChanged("") },
                onScan = {
                    keyboardController?.hide()
                    viewModel.startAudit()
                },
                isScanning = uiState.scanStatus == ScanStatus.SCANNING
            )

            // Preset suggestions
            SampleUrlChips(
                onSelectUrl = { selected ->
                    viewModel.onUrlChanged(selected)
                },
                enabled = uiState.scanStatus != ScanStatus.SCANNING
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons (Start Audit, Save, Share)
            ActionControlsRow(
                isScanning = uiState.scanStatus == ScanStatus.SCANNING,
                hasResults = uiState.logs.isNotEmpty(),
                onStartScan = {
                    keyboardController?.hide()
                    viewModel.startAudit()
                },
                onCancelScan = { viewModel.cancelAudit() },
                onSaveReport = { viewModel.saveReport(context) },
                onShareReport = { viewModel.shareReport(context) },
                onCopyReport = { viewModel.copyReportToClipboard(context) },
                onClear = { viewModel.clearResults() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Indicator Banner
            StatusBanner(
                statusText = uiState.statusText,
                severity = uiState.statusSeverity,
                isScanning = uiState.scanStatus == ScanStatus.SCANNING
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs (Findings vs Console Log)
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = SamiCardBg,
                contentColor = SamiGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = SamiGold
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, SamiCardBorder, RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = {
                        Text(
                            text = "SECURITY FINDINGS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (uiState.selectedTab == 0) SamiGold else SamiMuted
                        )
                    },
                    modifier = Modifier.testTag("tab_findings")
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CONSOLE LOG",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (uiState.selectedTab == 1) SamiGold else SamiMuted
                            )
                            if (uiState.logs.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(SamiCardElevated, CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${uiState.logs.size}",
                                        fontSize = 10.sp,
                                        color = SamiGold
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_console")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content Area based on selected tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.selectedTab == 0) {
                    FindingsView(
                        report = uiState.report,
                        isScanning = uiState.scanStatus == ScanStatus.SCANNING,
                        hasLogs = uiState.logs.isNotEmpty()
                    )
                } else {
                    ConsoleLogView(logs = uiState.logs)
                }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Professional Cyber Emblem Logo
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
                text = "ADVANCED SECURITY AUDITOR",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = SamiMuted
            )
        }
    }
}

@Composable
private fun UrlInputField(
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
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Security Target",
                tint = SamiGold,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (url.isNotEmpty() && !isScanning) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.testTag("clear_url_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear URL",
                        tint = SamiMuted
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(
            onGo = { onScan() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SamiCardBg,
            unfocusedContainerColor = SamiCardBg,
            focusedTextColor = SamiText,
            unfocusedTextColor = SamiText,
            focusedBorderColor = SamiGold,
            unfocusedBorderColor = SamiCardBorder
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("url_input_field")
    )
}

@Composable
private fun SampleUrlChips(
    onSelectUrl: (String) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val samples = listOf("example.com", "owasp.org", "google.com")
        Text(
            text = "Presets:",
            color = SamiMuted,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        samples.forEach { sample ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SamiCardElevated)
                    .border(0.8.dp, SamiCardBorder, RoundedCornerShape(6.dp))
                    .clickable(enabled = enabled) { onSelectUrl(sample) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = sample,
                    color = SamiGold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ActionControlsRow(
    isScanning: Boolean,
    hasResults: Boolean,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onSaveReport: () -> Unit,
    onShareReport: () -> Unit,
    onCopyReport: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Action Button (START AUDIT / STOP)
            if (isScanning) {
                Button(
                    onClick = onCancelScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SamiBad,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.stop_audit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                Button(
                    onClick = onStartScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SamiGold,
                        contentColor = Color(0xFF090E13)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("start_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Start",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.start_audit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Save Report Button
            Button(
                onClick = onSaveReport,
                enabled = hasResults && !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SamiCardBg,
                    contentColor = SamiText,
                    disabledContainerColor = SamiCardBg.copy(alpha = 0.5f),
                    disabledContentColor = SamiMuted.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(1.dp, if (hasResults && !isScanning) SamiGold else SamiCardBorder, RoundedCornerShape(8.dp))
                    .testTag("save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Report",
                    modifier = Modifier.size(16.dp),
                    tint = if (hasResults && !isScanning) SamiGold else SamiMuted
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.save_report),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Secondary actions (Share, Copy, Clear) when results exist
        if (hasResults && !isScanning) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShareReport,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SamiGold),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onCopyReport,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SamiText),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("copy_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onClear,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SamiMuted),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("clear_button")
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(
    statusText: String,
    severity: LogSeverity,
    isScanning: Boolean
) {
    val statusColor = when (severity) {
        LogSeverity.GOOD -> SamiGood
        LogSeverity.BAD -> SamiBad
        LogSeverity.WARN -> SamiWarn
        LogSeverity.HEADER -> SamiGold
        LogSeverity.INFO -> SamiMuted
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SamiCardBg)
            .border(1.dp, SamiCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }

        if (isScanning) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                color = SamiGold,
                trackColor = SamiCardElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun FindingsView(
    report: AuditReport?,
    isScanning: Boolean,
    hasLogs: Boolean
) {
    if (report == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Analyzing security posture...",
                        color = SamiGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Probing headers, certificates & endpoints",
                        color = SamiMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SamiCardBorder,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No audit report yet",
                        color = SamiMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enter a URL above and tap START AUDIT",
                        color = SamiMuted.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Score & Posture Summary Card
        item {
            ScoreCard(report = report)
        }

        // Target Info Card
        item {
            TargetSummaryCard(target = report.target)
        }

        // Hardening & Security Headers
        item {
            HeadersCard(headers = report.securityHeaders)
        }

        // Tech & CORS Analysis
        item {
            TechCorsCard(techCors = report.techCors)
        }

        // Cookie Flags Audit
        item {
            CookiesCard(cookies = report.cookies)
        }

        // Sensitive Paths Recon
        item {
            SensitivePathsCard(paths = report.sensitivePaths)
        }
    }
}

@Composable
private fun ScoreCard(report: AuditReport) {
    val score = report.score
    val (grade, gradeColor) = when {
        score >= 85 -> "A" to SamiGood
        score >= 70 -> "B" to SamiGold
        score >= 50 -> "C" to SamiWarn
        else -> "F" to SamiBad
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SamiCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SamiCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grade Circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(gradeColor.copy(alpha = 0.15f))
                    .border(2.dp, gradeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = grade,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = gradeColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SECURITY SCORE: $score/100",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SamiText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreBadge(label = "Passed", count = report.passedCount, color = SamiGood)
                    ScoreBadge(label = "Warnings", count = report.warningCount, color = SamiWarn)
                    ScoreBadge(label = "Critical", count = report.criticalCount, color = SamiBad)
                }
            }
        }
    }
}

@Composable
private fun ScoreBadge(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count $label",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TargetSummaryCard(target: com.sami.auditor.data.model.TargetSummary) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SamiCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SamiCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionHeader(title = "TARGET SUMMARY", icon = Icons.Default.Security)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Target URL", target.requestedUrl)
            InfoRow("Final Resolved URL", target.finalUrl)
            InfoRow("HTTP Status Code", "${target.responseCode ?: "N/A"}")
            InfoRow("Latency", "${String.format("%.2f", target.elapsedTimeSeconds)}s")
            InfoRow(
                "HTTPS Encryption",
                if (target.isHttps) "Enabled (Secure)" else "Disabled (Insecure)",
                valueColor = if (target.isHttps) SamiGood else SamiBad
            )
        }
    }
}

@Composable
private fun HeadersCard(headers: List<SecurityHeaderItem>) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SamiCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SamiCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionHeader(title = "HARDENING & HEADERS", icon = Icons.Default.Lock)
            Spacer(modifier = Modifier.height(8.dp))
            headers.forEachIndexed { index, header ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (header.isPresent) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = if (header.isPresent) "Present" else "Missing",
                        tint = if (header.isPresent) SamiGood else SamiBad,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = header.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SamiText
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (header.isPresent) "PRESENT" else "MISSING",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = if (header.isPresent) SamiGood else SamiBad
                            )
                        }
                        Text(
                            text = header.explanationEn,
                            fontSize = 11.sp,
                            color = SamiMuted
                        )
                        Text(
                            text = header.explanationAr,
                            fontSize = 11.sp,
                            color = SamiGold.copy(alpha = 0.8f)
                        )
                        if (!header.rawValue.isNullOrBlank()) {
                            Text(
                                text = "Value: ${header.rawValue}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SamiInfo,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                if (index < headers.lastIndex) {
                    HorizontalDivider(color = SamiCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun TechCorsCard(techCors: TechCorsFinding) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SamiCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SamiCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionHeader(title = "TECH & CORS AUDIT", icon = Icons.Default.BugReport)
            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(
                "Server Banner",
                if (!techCors.serverBanner.isNullOrBlank()) techCors.serverBanner else "Hidden (Safe)",
                valueColor = if (!techCors.serverBanner.isNullOrBlank()) SamiWarn else SamiGood
            )
            InfoRow(
                "Framework / X-Powered-By",
                if (!techCors.xPoweredBy.isNullOrBlank()) "Leaked: ${techCors.xPoweredBy}" else "None Detected (Safe)",
                valueColor = if (!techCors.xPoweredBy.isNullOrBlank()) SamiBad else SamiGood
            )
            InfoRow(
                "CORS Policy",
                when {
                    techCors.isCorsWildcard -> "Wildcard (*) - High Exposure Risk"
                    !techCors.corsHeader.isNullOrBlank() -> "Restricted (${techCors.corsHeader})"
                    else -> "Not Specified"
                },
                valueColor = when {
                    techCors.isCorsWildcard -> SamiBad
                    !techCors.corsHeader.isNullOrBlank() -> SamiGood
                    else -> SamiMuted
                }
            )
        }
    }
}

@Composable
private fun CookiesCard(cookies: CookieFinding) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SamiCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SamiCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionHeader(title = "COOKIE FLAGS AUDIT", icon = Icons.Default.LockOpen)
            Spacer(modifier = Modifier.height(8.dp))

            if (!cookies.hasCookies) {
                Text(
                    text = "No Set-Cookie headers detected on initial response.",
                    color = SamiMuted,
                    fontSize = 12.sp
                )
            } else {
                InfoRow("Cookies Detected", "${cookies.cookieCount}")
                InfoRow(
                    "HttpOnly Flag",
                    if (cookies.httpOnlySet) "Set (Protected against XSS theft)" else "Missing (Risk of XSS Cookie Stealing)",
                    valueColor = if (cookies.httpOnlySet) SamiGood else SamiBad
                )
                InfoRow(
                    "Secure Flag",
                    if (cookies.secureSet) "Set (HTTPS Only)" else "Missing (Transmitted over plain HTTP)",
                    valueColor = if (cookies.secureSet) SamiGood else SamiBad
                )
                InfoRow(
                    "SameSite Flag",
                    if (cookies.sameSiteConfigured) "Configured (CSRF Mitigation)" else "Missing (CSRF Exposure)",
                    valueColor = if (cookies.sameSiteConfigured) SamiGood else SamiWarn
                )
            }
        }
    }
}

@Composable
private fun SensitivePathsCard(paths: List<PathProbeResult>) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SamiCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SamiCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionHeader(title = "SENSITIVE PATHS CHECK", icon = Icons.Default.Warning)
            Spacer(modifier = Modifier.height(8.dp))

            paths.forEachIndexed { index, probe ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when {
                        probe.isAccessible -> "[HTTP ${probe.statusCode}] ACCESSIBLE"
                        probe.statusCode != null -> "[HTTP ${probe.statusCode}]"
                        else -> "[ERR]"
                    }
                    val statusColor = when {
                        probe.isAccessible -> SamiBad
                        probe.statusCode != null -> SamiGood
                        else -> SamiMuted
                    }

                    Text(
                        text = "/${probe.path}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = SamiText,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = statusText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = statusColor
                    )
                }
                if (index < paths.lastIndex) {
                    HorizontalDivider(color = SamiCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SamiGold,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            color = SamiGold
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = SamiText
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = SamiMuted
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun ConsoleLogView(logs: List<AuditLogEntry>) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF06090D))
            .border(1.dp, SamiCardBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No console output. Start audit to stream results.",
                    color = SamiMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    val color = when (log.severity) {
                        LogSeverity.HEADER -> SamiGold
                        LogSeverity.GOOD -> SamiGood
                        LogSeverity.BAD -> SamiBad
                        LogSeverity.WARN -> SamiWarn
                        LogSeverity.INFO -> SamiMuted
                    }
                    Text(
                        text = log.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = if (log.isBold) FontWeight.Bold else FontWeight.Normal,
                        color = color
                    )
                }
            }
        }
    }
}
