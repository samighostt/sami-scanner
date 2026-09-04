package com.sami.auditor.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sami.auditor.data.export.ReportExporter
import com.sami.auditor.data.model.AuditHistoryItem
import com.sami.auditor.data.model.AuditLogEntry
import com.sami.auditor.data.model.AuditReport
import com.sami.auditor.data.model.LogSeverity
import com.sami.auditor.data.model.MonitoredSite
import com.sami.auditor.data.model.ScanStatus
import com.sami.auditor.data.model.SeverityLevel
import com.sami.auditor.data.model.VulnerabilityFinding
import com.sami.auditor.data.network.SecurityScanner
import com.sami.auditor.data.storage.AuditStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class ExportFormat {
    HTML,
    JSON,
    TEXT
}

data class AuditUiState(
    val urlInput: String = "",
    val statusText: String = "جاهز لبدء الفحص والتدقيق الأمني • Ready to audit",
    val statusSeverity: LogSeverity = LogSeverity.INFO,
    val scanStatus: ScanStatus = ScanStatus.IDLE,
    val logs: List<AuditLogEntry> = emptyList(),
    val report: AuditReport? = null,
    val selectedTab: Int = 0, // 0: Scanner, 1: Vulnerabilities, 2: SSL & Paths, 3: Dashboard, 4: Monitoring, 5: Console
    val selectedSeverityFilter: SeverityLevel? = null,
    val selectedFindingForRemediation: VulnerabilityFinding? = null,
    val history: List<AuditHistoryItem> = emptyList(),
    val monitoredSites: List<MonitoredSite> = emptyList(),
    val showExportDialog: Boolean = false,
    val snackbarMessage: String? = null
)

class AuditViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val scanner = SecurityScanner()
    private val storage = AuditStorage(application.applicationContext)

    private val _uiState = MutableStateFlow(AuditUiState())
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    private var auditJob: Job? = null

    init {
        loadSavedData()
    }

    private fun loadSavedData() {
        val history = storage.getScanHistory()
        val monitored = storage.getMonitoredSites()
        _uiState.update {
            it.copy(
                history = history,
                monitoredSites = monitored
            )
        }
    }

    fun onUrlChanged(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl) }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun setSeverityFilter(severity: SeverityLevel?) {
        _uiState.update { it.copy(selectedSeverityFilter = severity) }
    }

    fun openRemediationDialog(finding: VulnerabilityFinding) {
        _uiState.update { it.copy(selectedFindingForRemediation = finding) }
    }

    fun dismissRemediationDialog() {
        _uiState.update { it.copy(selectedFindingForRemediation = null) }
    }

    fun toggleExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearResults() {
        auditJob?.cancel()
        _uiState.update {
            it.copy(
                logs = emptyList(),
                report = null,
                scanStatus = ScanStatus.IDLE,
                statusText = "جاهز لبدء الفحص • Ready",
                statusSeverity = LogSeverity.INFO
            )
        }
    }

    fun startAudit(overrideUrl: String? = null) {
        val url = (overrideUrl ?: _uiState.value.urlInput).trim()
        if (url.isEmpty()) {
            _uiState.update {
                it.copy(
                    statusText = "الرجاء إدخال رابط صالح • Please enter a valid URL",
                    statusSeverity = LogSeverity.BAD
                )
            }
            return
        }

        auditJob?.cancel()
        _uiState.update {
            it.copy(
                urlInput = url,
                logs = emptyList(),
                report = null,
                scanStatus = ScanStatus.SCANNING,
                statusText = "جاري الفحص المتقدم واختبار OWASP و SSL... • Scanning...",
                statusSeverity = LogSeverity.WARN
            )
        }

        auditJob = viewModelScope.launch {
            try {
                val reportResult = scanner.audit(url) { newLog ->
                    _uiState.update { state ->
                        state.copy(logs = state.logs + newLog)
                    }
                }
                storage.saveScanHistory(reportResult)
                val updatedHistory = storage.getScanHistory()

                _uiState.update {
                    it.copy(
                        scanStatus = ScanStatus.COMPLETED,
                        statusText = "اكتمل الفحص بنجاح • Audit Completed (${reportResult.score}/100)",
                        statusSeverity = LogSeverity.GOOD,
                        report = reportResult,
                        history = updatedHistory
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Audit failed"
                _uiState.update {
                    it.copy(
                        scanStatus = ScanStatus.FAILED,
                        statusText = "فشل الفحص: $errorMsg • Failed",
                        statusSeverity = LogSeverity.BAD
                    )
                }
            }
        }
    }

    fun cancelAudit() {
        auditJob?.cancel()
        _uiState.update {
            it.copy(
                scanStatus = ScanStatus.IDLE,
                statusText = "تم إلغاء الفحص • Audit Cancelled",
                statusSeverity = LogSeverity.WARN
            )
        }
    }

    // Exporting
    fun exportReport(context: Context, format: ExportFormat) {
        val report = _uiState.value.report
        if (report == null) {
            _uiState.update { it.copy(snackbarMessage = "لا يوجد تقرير لتصديره حالياً • No report to export") }
            return
        }

        try {
            when (format) {
                ExportFormat.HTML -> {
                    val htmlContent = ReportExporter.buildHtmlReport(report)
                    val filename = "sami_audit_${System.currentTimeMillis()}.html"
                    val file = storage.writeReportFile(context, filename, htmlContent)
                    shareFile(context, file, "text/html", "SAMI Security Audit Report (HTML)")
                    _uiState.update {
                        it.copy(
                            showExportDialog = false,
                            snackbarMessage = "تم إنشاء التقرير بصيغة HTML جاهز للمشاركة أو الطباعة كـ PDF!"
                        )
                    }
                }
                ExportFormat.JSON -> {
                    val jsonContent = ReportExporter.buildJsonReport(report)
                    val filename = "sami_audit_${System.currentTimeMillis()}.json"
                    val file = storage.writeReportFile(context, filename, jsonContent)
                    shareFile(context, file, "application/json", "SAMI Security Audit Report (JSON)")
                    _uiState.update {
                        it.copy(
                            showExportDialog = false,
                            snackbarMessage = "تم تصدير التقرير بصيغة JSON بنجاح!"
                        )
                    }
                }
                ExportFormat.TEXT -> {
                    val textContent = ReportExporter.buildSummaryText(report)
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, textContent)
                        putExtra(Intent.EXTRA_SUBJECT, "SAMI Security Audit - ${report.target.requestedUrl}")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "مشاركة ملخص التقرير")
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(shareIntent)
                    _uiState.update { it.copy(showExportDialog = false) }
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(snackbarMessage = "خطأ في التصدير: ${e.message}") }
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to text share
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, file.readText())
                type = "text/plain"
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun copyReportToClipboard(context: Context) {
        val report = _uiState.value.report
        val text = if (report != null) ReportExporter.buildSummaryText(report) else "No report data"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SAMI Audit Report", text)
        clipboard.setPrimaryClip(clip)
        _uiState.update { it.copy(snackbarMessage = "تم نسخ التقرير إلى الحافظة بنجاح!") }
    }

    fun copySnippetToClipboard(context: Context, snippet: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Remediation Config", snippet)
        clipboard.setPrimaryClip(clip)
        _uiState.update { it.copy(snackbarMessage = "تم نسخ كود الإصلاح إلى الحافظة!") }
    }

    // Monitoring
    fun addMonitoredSite(url: String, intervalHours: Int = 24) {
        if (url.isBlank()) return
        val updated = storage.addMonitoredSite(url.trim(), intervalHours)
        _uiState.update {
            it.copy(
                monitoredSites = updated,
                snackbarMessage = "تمت إضافة الموقع لجدول المراقبة الدورية!"
            )
        }
    }

    fun removeMonitoredSite(id: String) {
        val updated = storage.removeMonitoredSite(id)
        _uiState.update {
            it.copy(
                monitoredSites = updated,
                snackbarMessage = "تم حذف الموقع من المراقبة"
            )
        }
    }

    fun checkMonitoredSiteNow(site: MonitoredSite) {
        _uiState.update { it.copy(selectedTab = 0) }
        startAudit(site.url)
    }

    fun clearHistory() {
        storage.clearHistory()
        _uiState.update {
            it.copy(
                history = emptyList(),
                snackbarMessage = "تم مسح سجل الفحوصات"
            )
        }
    }
}
