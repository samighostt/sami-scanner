package com.sami.auditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sami.auditor.R
import com.sami.auditor.data.model.AuditLogEntry
import com.sami.auditor.data.model.AuditReport
import com.sami.auditor.data.model.LogSeverity
import com.sami.auditor.data.model.ScanStatus
import com.sami.auditor.ui.components.ActionControlsRow
import com.sami.auditor.ui.components.AppHeader
import com.sami.auditor.ui.components.DashboardView
import com.sami.auditor.ui.components.ExportReportDialog
import com.sami.auditor.ui.components.MonitoringView
import com.sami.auditor.ui.components.RemediationDialog
import com.sami.auditor.ui.components.SampleUrlChips
import com.sami.auditor.ui.components.SslAndPathsView
import com.sami.auditor.ui.components.StatusBanner
import com.sami.auditor.ui.components.UrlInputField
import com.sami.auditor.ui.components.VulnerabilitiesView
import com.sami.auditor.ui.theme.SamiBackground
import com.sami.auditor.ui.theme.SamiBad
import com.sami.auditor.ui.theme.SamiCardBg
import com.sami.auditor.ui.theme.SamiCardBorder
import com.sami.auditor.ui.theme.SamiGold
import com.sami.auditor.ui.theme.SamiGood
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
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAMI Cybersecurity Auditor • للمراجعة الأمنية الأخلاقية فقط",
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
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Header with Cyber Emblem and Export button
            AppHeader(
                hasReport = uiState.report != null,
                onOpenExport = { viewModel.toggleExportDialog(true) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // URL input box
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

            // Preset fast sample chips
            SampleUrlChips(
                onSelectUrl = { sample ->
                    viewModel.onUrlChanged(sample)
                    viewModel.startAudit(sample)
                },
                enabled = uiState.scanStatus != ScanStatus.SCANNING
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action controls (Start, Cancel, Export, Copy, Clear)
            ActionControlsRow(
                isScanning = uiState.scanStatus == ScanStatus.SCANNING,
                hasResults = uiState.report != null,
                onStartScan = {
                    keyboardController?.hide()
                    viewModel.startAudit()
                },
                onCancelScan = { viewModel.cancelAudit() },
                onOpenExport = { viewModel.toggleExportDialog(true) },
                onCopyReport = { viewModel.copyReportToClipboard(context) },
                onClear = { viewModel.clearResults() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Live Status Banner
            StatusBanner(
                statusText = uiState.statusText,
                severity = uiState.statusSeverity,
                isScanning = uiState.scanStatus == ScanStatus.SCANNING
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs (Scan, Vulns, SSL & Paths, Dashboard, Monitoring, Console)
            AuditTabs(
                selectedTab = uiState.selectedTab,
                onSelectTab = { viewModel.setSelectedTab(it) },
                vulnCount = uiState.report?.vulnerabilities?.size ?: 0,
                logCount = uiState.logs.size
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Tab Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState.selectedTab) {
                    0 -> ScanSummaryView(
                        report = uiState.report,
                        isScanning = uiState.scanStatus == ScanStatus.SCANNING,
                        onViewVulns = { viewModel.setSelectedTab(1) },
                        onViewSsl = { viewModel.setSelectedTab(2) }
                    )
                    1 -> VulnerabilitiesView(
                        report = uiState.report,
                        selectedSeverity = uiState.selectedSeverityFilter,
                        onSelectSeverity = { viewModel.setSeverityFilter(it) },
                        onOpenRemediation = { viewModel.openRemediationDialog(it) }
                    )
                    2 -> SslAndPathsView(report = uiState.report)
                    3 -> DashboardView(
                        history = uiState.history,
                        onReScan = { url ->
                            viewModel.setSelectedTab(0)
                            viewModel.startAudit(url)
                        },
                        onClearHistory = { viewModel.clearHistory() }
                    )
                    4 -> MonitoringView(
                        monitoredSites = uiState.monitoredSites,
                        onAddSite = { url, interval -> viewModel.addMonitoredSite(url, interval) },
                        onRemoveSite = { id -> viewModel.removeMonitoredSite(id) },
                        onCheckSiteNow = { site -> viewModel.checkMonitoredSiteNow(site) }
                    )
                    5 -> ConsoleLogView(logs = uiState.logs)
                }
            }
        }
    }

    // Export Dialog
    if (uiState.showExportDialog) {
        ExportReportDialog(
            onDismiss = { viewModel.toggleExportDialog(false) },
            onSelectFormat = { format ->
                viewModel.exportReport(context, format)
            }
        )
    }

    // Remediation Dialog
    uiState.selectedFindingForRemediation?.let { finding ->
        RemediationDialog(
            finding = finding,
            onDismiss = { viewModel.dismissRemediationDialog() },
            onCopySnippet = { snippet ->
                viewModel.copySnippetToClipboard(context, snippet)
            }
        )
    }
}

@Composable
private fun AuditTabs(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    vulnCount: Int,
    logCount: Int
) {
    val tabs = listOf(
        TabData("الفحص", Icons.Default.Security, null),
        TabData("الثغرات", Icons.Default.BugReport, if (vulnCount > 0) "$vulnCount" else null),
        TabData("SSL والمسارات", Icons.Default.Https, null),
        TabData("لوحة التحكم", Icons.Default.BarChart, null),
        TabData("المراقبة", Icons.Default.Schedule, null),
        TabData("السجل", Icons.Default.Code, if (logCount > 0) "$logCount" else null)
    )

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = SamiCardBg,
        contentColor = SamiGold,
        edgePadding = 4.dp,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = SamiGold,
                height = 2.dp
            )
        },
        divider = { HorizontalDivider(color = SamiCardBorder, thickness = 1.dp) },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, SamiCardBorder, RoundedCornerShape(8.dp))
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedTab == index,
                onClick = { onSelectTab(index) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (selectedTab == index) SamiGold else SamiMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tab.title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (selectedTab == index) SamiGold else SamiMuted
                        )
                        if (tab.badge != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (index == 1) SamiBad else SamiCardBorder)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = tab.badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("tab_$index")
            )
        }
    }
}

private data class TabData(
    val title: String,
    val icon: ImageVector,
    val badge: String?
)

@Composable
private fun ScanSummaryView(
    report: AuditReport?,
    isScanning: Boolean,
    onViewVulns: () -> Unit,
    onViewSsl: () -> Unit
) {
    if (report == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "جاري الفحص المتقدم واكتشاف الثغرات...",
                        color = SamiGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يتم الآن فحص OWASP Top 10 و SSL و المسارات وتحديد الطلبات",
                        color = SamiMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SamiCardBorder,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "أدخل رابط الموقع واضغط 'بدء الفحص الأمني'",
                        color = SamiMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main Security Score Banner
        item {
            ScoreBanner(report = report)
        }

        // Target Resolved Info
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SamiCardBg),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SamiCardBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "تفاصيل الهدف (Target Details)",
                        color = SamiGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryRow("الرابط المدخل", report.target.requestedUrl)
                    SummaryRow("الرابط النهائي", report.target.finalUrl)
                    SummaryRow("رمز الاستجابة", "HTTP ${report.target.responseCode ?: "N/A"}")
                    SummaryRow("زمن الاستجابة", "${String.format("%.2f", report.target.elapsedTimeSeconds)} ثانية")
                    SummaryRow("تشفير HTTPS", if (report.target.isHttps) "مفعل وآمن" else "معطل (غير آمن)", if (report.target.isHttps) SamiGood else SamiBad)
                }
            }
        }

        // Executive Breakdown Highlights
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SamiCardBg),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SamiCardBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ملخص الفحص الأمني",
                        color = SamiGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScoreStatChip("ثغرات مكتشفة", "${report.vulnerabilities.size}", if (report.vulnerabilities.isNotEmpty()) SamiBad else SamiGood)
                        ScoreStatChip("شهادة SSL", if (report.sslInfo.isHttps) "صالحة" else "مفقودة", if (report.sslInfo.isHttps) SamiGood else SamiBad)
                        ScoreStatChip("مسارات مكشوفة", "${report.sensitivePaths.count { it.isAccessible }}", if (report.sensitivePaths.any { it.isAccessible }) SamiBad else SamiGood)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScoreBanner(report: AuditReport) {
    val score = report.score
    val (grade, gradeColor) = when {
        score >= 85 -> "A" to SamiGood
        score >= 70 -> "B" to SamiGold
        score >= 50 -> "C" to SamiWarn
        else -> "F" to SamiBad
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SamiCardBg)
            .border(1.dp, SamiCardBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(gradeColor.copy(alpha = 0.15f))
                    .border(2.dp, gradeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = grade,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = gradeColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "درجة الأمان الإجمالية: $score/100",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SamiText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScoreBadge(label = "ناجح", count = report.passedCount, color = SamiGood)
                    ScoreBadge(label = "تحذير", count = report.warningCount, color = SamiWarn)
                    ScoreBadge(label = "حرج", count = report.criticalCount, color = SamiBad)
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
private fun ScoreStatChip(title: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SamiBackground)
            .border(1.dp, SamiCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(text = title, color = SamiMuted, fontSize = 10.sp)
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = SamiText
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = SamiMuted)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            fontFamily = FontFamily.Monospace
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
                    text = "لا توجد سجلات بعد. ابدأ الفحص لبث السجلات الحية.",
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
