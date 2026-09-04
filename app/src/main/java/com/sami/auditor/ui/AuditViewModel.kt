package com.sami.auditor.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sami.auditor.data.model.AuditLogEntry
import com.sami.auditor.data.model.AuditReport
import com.sami.auditor.data.model.LogSeverity
import com.sami.auditor.data.model.ScanStatus
import com.sami.auditor.data.network.SecurityScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class AuditUiState(
    val urlInput: String = "",
    val statusText: String = "Ready to perform security audit",
    val statusSeverity: LogSeverity = LogSeverity.INFO,
    val scanStatus: ScanStatus = ScanStatus.IDLE,
    val logs: List<AuditLogEntry> = emptyList(),
    val report: AuditReport? = null,
    val selectedTab: Int = 0,
    val snackbarMessage: String? = null
)

class AuditViewModel(
    private val scanner: SecurityScanner = SecurityScanner()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditUiState())
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    private var auditJob: Job? = null

    fun onUrlChanged(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl) }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
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
                statusText = "Ready to perform security audit",
                statusSeverity = LogSeverity.INFO
            )
        }
    }

    fun startAudit() {
        val url = _uiState.value.urlInput.trim()
        if (url.isEmpty()) {
            _uiState.update {
                it.copy(
                    statusText = "Please enter a valid URL.",
                    statusSeverity = LogSeverity.BAD
                )
            }
            return
        }

        auditJob?.cancel()
        _uiState.update {
            it.copy(
                logs = emptyList(),
                report = null,
                scanStatus = ScanStatus.SCANNING,
                statusText = "Running comprehensive analysis...",
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
                _uiState.update {
                    it.copy(
                        scanStatus = ScanStatus.COMPLETED,
                        statusText = "Audit completed successfully.",
                        statusSeverity = LogSeverity.GOOD,
                        report = reportResult
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Audit failed."
                _uiState.update {
                    it.copy(
                        scanStatus = ScanStatus.FAILED,
                        statusText = "Audit failed: $errorMsg",
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
                statusText = "Audit cancelled.",
                statusSeverity = LogSeverity.WARN
            )
        }
    }

    fun generateReportText(): String {
        val state = _uiState.value
        val report = state.report
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("            SAMI SECURITY AUDITOR REPORT          ")
        sb.appendLine("==================================================")
        sb.appendLine("Date: ${java.util.Date()}")
        sb.appendLine("Target URL: ${report?.target?.requestedUrl ?: state.urlInput}")
        sb.appendLine("Final URL: ${report?.target?.finalUrl ?: "N/A"}")
        sb.appendLine("Status Code: ${report?.target?.responseCode ?: "N/A"}")
        sb.appendLine("Response Time: ${String.format("%.2f", report?.target?.elapsedTimeSeconds ?: 0.0)}s")
        sb.appendLine("HTTPS Encryption: ${if (report?.target?.isHttps == true) "Enabled" else "Insecure/Disabled"}")
        sb.appendLine("Overall Score: ${report?.score ?: 0}/100")
        sb.appendLine()
        sb.appendLine("--- SECURITY HEADERS ---")
        report?.securityHeaders?.forEach { h ->
            sb.appendLine("[${if (h.isPresent) "+" else "-"}] ${h.name} : ${if (h.isPresent) "PRESENT" else "MISSING"}")
            sb.appendLine("    Description: ${h.explanationEn} (${h.explanationAr})")
            if (!h.rawValue.isNullOrBlank()) {
                sb.appendLine("    Value: ${h.rawValue}")
            }
        }
        sb.appendLine()
        sb.appendLine("--- SERVER & CORS ---")
        sb.appendLine("Server Banner: ${report?.techCors?.serverBanner ?: "Hidden [SAFE]"}")
        sb.appendLine("Technology Leak: ${report?.techCors?.xPoweredBy ?: "None detected [SAFE]"}")
        sb.appendLine("CORS Header: ${report?.techCors?.corsHeader ?: "Not configured"}")
        sb.appendLine()
        sb.appendLine("--- COOKIES ---")
        if (report?.cookies?.hasCookies == true) {
            sb.appendLine("Detected Cookies: ${report.cookies.cookieCount}")
            sb.appendLine("HttpOnly: ${if (report.cookies.httpOnlySet) "Set [SAFE]" else "Missing [XSS RISK]"}")
            sb.appendLine("Secure: ${if (report.cookies.secureSet) "Set [SAFE]" else "Missing [INSECURE]"}")
            sb.appendLine("SameSite: ${if (report.cookies.sameSiteConfigured) "Configured [SAFE]" else "Missing"}")
        } else {
            sb.appendLine("No Set-Cookie headers detected.")
        }
        sb.appendLine()
        sb.appendLine("--- SENSITIVE PATHS AUDIT ---")
        report?.sensitivePaths?.forEach { p ->
            val status = when {
                p.isAccessible -> "ACCESSIBLE [CRITICAL RISK]"
                p.statusCode != null -> "HTTP ${p.statusCode} [BLOCKED/SAFE]"
                else -> "ERROR (${p.errorMessage ?: "Failed"})"
            }
            sb.appendLine("/${p.path} -> $status")
        }
        sb.appendLine()
        sb.appendLine("--- CONSOLE AUDIT LOGS ---")
        state.logs.forEach { log ->
            sb.appendLine(log.text)
        }
        sb.appendLine("==================================================")
        sb.appendLine("SAMI AUDITOR • Authorized testing & security research only")
        return sb.toString()
    }

    fun saveReport(context: Context) {
        try {
            val reportText = generateReportText()
            val file = File(context.filesDir, "sami_security_report.txt")
            file.writeText(reportText, Charsets.UTF_8)
            _uiState.update {
                it.copy(snackbarMessage = "Report saved to app storage (${file.name})")
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(snackbarMessage = "Failed to save report: ${e.message}")
            }
        }
    }

    fun shareReport(context: Context) {
        try {
            val reportText = generateReportText()
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, reportText)
                putExtra(Intent.EXTRA_SUBJECT, "SAMI Security Audit Report - ${_uiState.value.urlInput}")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Security Report")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(snackbarMessage = "Could not open share dialog: ${e.message}")
            }
        }
    }

    fun copyReportToClipboard(context: Context) {
        try {
            val reportText = generateReportText()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("SAMI Audit Report", reportText)
            clipboard.setPrimaryClip(clip)
            _uiState.update {
                it.copy(snackbarMessage = "Report copied to clipboard!")
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(snackbarMessage = "Copy failed: ${e.message}")
            }
        }
    }
}
