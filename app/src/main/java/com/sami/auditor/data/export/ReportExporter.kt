package com.sami.auditor.data.export

import com.sami.auditor.data.model.AuditReport
import com.sami.auditor.data.model.SeverityLevel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    fun buildHtmlReport(report: AuditReport): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val formattedDate = dateFormat.format(Date(report.timestamp))
        val scoreColor = when {
            report.score >= 80 -> "#10B981"
            report.score >= 50 -> "#F59E0B"
            else -> "#EF4444"
        }

        val vulnerabilitiesHtml = report.vulnerabilities.joinToString("\n") { vuln ->
            val badgeBg = when (vuln.severity) {
                SeverityLevel.CRITICAL -> "#EF4444"
                SeverityLevel.HIGH -> "#F97316"
                SeverityLevel.MEDIUM -> "#F59E0B"
                SeverityLevel.LOW -> "#3B82F6"
                SeverityLevel.INFO -> "#6B7280"
                SeverityLevel.PASSED -> "#10B981"
            }
            val codeSection = if (!vuln.configSnippet.isNullOrBlank()) {
                """
                <div class="code-box">
                    <div class="code-title">Recommended Fix / التكوين المقترح:</div>
                    <pre><code>${escapeHtml(vuln.configSnippet)}</code></pre>
                </div>
                """.trimIndent()
            } else ""

            """
            <div class="card vuln-card">
                <div class="vuln-header">
                    <div>
                        <span class="badge" style="background-color: $badgeBg;">${vuln.severity.labelEn} (CVSS ${vuln.cvssScore})</span>
                        <span class="category-tag">${escapeHtml(vuln.owaspCategory)}</span>
                    </div>
                </div>
                <h3 class="vuln-title">${escapeHtml(vuln.titleEn)} - ${escapeHtml(vuln.titleAr)}</h3>
                <p class="vuln-desc">${escapeHtml(vuln.descriptionEn)}</p>
                <p class="vuln-desc-ar">${escapeHtml(vuln.descriptionAr)}</p>
                
                <div class="remediation-box">
                    <strong>Remediation / نصيحة الإصلاح:</strong>
                    <p>${escapeHtml(vuln.remediationEn)}</p>
                    <p style="direction: rtl; text-align: right;">${escapeHtml(vuln.remediationAr)}</p>
                </div>
                $codeSection
            </div>
            """.trimIndent()
        }

        val headersHtml = report.securityHeaders.joinToString("\n") { h ->
            val statusColor = if (h.isPresent) "#10B981" else "#EF4444"
            val statusText = if (h.isPresent) "PRESENT" else "MISSING"
            val raw = if (!h.rawValue.isNullOrBlank()) "<div class='header-val'><code>${escapeHtml(h.rawValue)}</code></div>" else ""
            """
            <tr style="border-bottom: 1px solid #1E293B;">
                <td style="padding: 10px; font-family: monospace; color: #E2E8F0;">${escapeHtml(h.name)}</td>
                <td style="padding: 10px; color: $statusColor; font-weight: bold;">$statusText</td>
                <td style="padding: 10px; color: #94A3B8; font-size: 13px;">${escapeHtml(h.explanationEn)}<br/><span style="direction: rtl;">${escapeHtml(h.explanationAr)}</span>$raw</td>
            </tr>
            """.trimIndent()
        }

        val pathsHtml = report.sensitivePaths.joinToString("\n") { p ->
            val statusColor = when {
                p.isAccessible -> "#EF4444"
                p.statusCode == 403 -> "#F59E0B"
                p.statusCode == 404 -> "#10B981"
                else -> "#94A3B8"
            }
            val statusLabel = when {
                p.isAccessible -> "EXPOSED (${p.statusCode})"
                p.statusCode != null -> "HTTP ${p.statusCode}"
                else -> "UNREACHABLE"
            }
            """
            <tr style="border-bottom: 1px solid #1E293B;">
                <td style="padding: 8px 12px; font-family: monospace; color: #F1F5F9;">/${escapeHtml(p.path)}</td>
                <td style="padding: 8px 12px; color: $statusColor; font-weight: bold;">$statusLabel</td>
                <td style="padding: 8px 12px; color: #94A3B8; font-size: 12px;">${escapeHtml(p.description)}</td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>SAMI Security Audit Report - ${escapeHtml(report.target.requestedUrl)}</title>
            <style>
                :root {
                    --bg: #0B111A;
                    --card-bg: #111B27;
                    --border: #1E2D40;
                    --text: #E2E8F0;
                    --text-muted: #94A3B8;
                    --gold: #D4AF37;
                    --gold-glow: rgba(212, 175, 55, 0.2);
                }
                body {
                    margin: 0;
                    padding: 24px;
                    background-color: var(--bg);
                    color: var(--text);
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    line-height: 1.6;
                }
                .container {
                    max-width: 960px;
                    margin: 0 auto;
                }
                .header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    border-bottom: 2px solid var(--border);
                    padding-bottom: 20px;
                    margin-bottom: 24px;
                }
                .logo-title h1 {
                    margin: 0;
                    font-size: 26px;
                    color: var(--gold);
                    letter-spacing: 1px;
                }
                .logo-title p {
                    margin: 4px 0 0 0;
                    color: var(--text-muted);
                    font-size: 13px;
                }
                .score-badge {
                    text-align: center;
                    background: var(--card-bg);
                    border: 2px solid $scoreColor;
                    border-radius: 12px;
                    padding: 12px 24px;
                }
                .score-num {
                    font-size: 36px;
                    font-weight: 800;
                    color: $scoreColor;
                }
                .score-label {
                    font-size: 11px;
                    text-transform: uppercase;
                    color: var(--text-muted);
                    letter-spacing: 1px;
                }
                .grid-2 {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 16px;
                    margin-bottom: 24px;
                }
                .card {
                    background: var(--card-bg);
                    border: 1px solid var(--border);
                    border-radius: 10px;
                    padding: 18px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                }
                .card h2 {
                    margin-top: 0;
                    font-size: 17px;
                    color: var(--gold);
                    border-bottom: 1px solid var(--border);
                    padding-bottom: 8px;
                }
                .detail-row {
                    display: flex;
                    justify-content: space-between;
                    padding: 6px 0;
                    font-size: 13px;
                    border-bottom: 1px dotted var(--border);
                }
                .detail-label { color: var(--text-muted); }
                .detail-val { font-weight: 600; font-family: monospace; }
                .vuln-card {
                    margin-bottom: 16px;
                    border-left: 4px solid var(--gold);
                }
                .vuln-header {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 8px;
                }
                .badge {
                    padding: 3px 8px;
                    border-radius: 4px;
                    font-size: 11px;
                    font-weight: bold;
                    color: #FFFFFF;
                }
                .category-tag {
                    font-size: 12px;
                    color: var(--text-muted);
                    margin-left: 8px;
                }
                .vuln-title {
                    margin: 6px 0;
                    font-size: 16px;
                    color: #F8FAFC;
                }
                .vuln-desc {
                    color: #CBD5E1;
                    font-size: 13px;
                    margin: 4px 0;
                }
                .vuln-desc-ar {
                    direction: rtl;
                    text-align: right;
                    color: #94A3B8;
                    font-size: 13px;
                    margin: 4px 0 10px 0;
                }
                .remediation-box {
                    background: rgba(212, 175, 55, 0.08);
                    border: 1px solid rgba(212, 175, 55, 0.25);
                    border-radius: 6px;
                    padding: 12px;
                    margin-top: 10px;
                    font-size: 13px;
                }
                .code-box {
                    background: #060A0F;
                    border-radius: 6px;
                    border: 1px solid var(--border);
                    padding: 12px;
                    margin-top: 10px;
                }
                .code-title {
                    font-size: 11px;
                    color: var(--gold);
                    margin-bottom: 6px;
                    text-transform: uppercase;
                }
                pre {
                    margin: 0;
                    font-family: monospace;
                    font-size: 12px;
                    color: #38BDF8;
                    overflow-x: auto;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    text-align: left;
                    font-size: 13px;
                }
                th {
                    background: #0E1622;
                    padding: 10px;
                    color: var(--gold);
                    border-bottom: 2px solid var(--border);
                }
                .footer {
                    margin-top: 40px;
                    text-align: center;
                    font-size: 12px;
                    color: var(--text-muted);
                    border-top: 1px solid var(--border);
                    padding-top: 20px;
                }
                @media print {
                    body { background: #FFF !important; color: #000 !important; }
                    .card { border: 1px solid #CCC; background: #FFF !important; box-shadow: none; color: #000 !important; }
                    .header { border-color: #000; }
                    .logo-title h1 { color: #000; }
                    table th { background: #EEE; color: #000; }
                    .code-box { background: #F8F8F8; border-color: #CCC; }
                    pre code { color: #000; }
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo-title">
                        <h1>SAMI SECURITY AUDITOR</h1>
                        <p>DEFENSIVE VULNERABILITY &amp; COMPLIANCE REPORT • OWASP TOP 10</p>
                    </div>
                    <div class="score-badge">
                        <div class="score-num">${report.score}</div>
                        <div class="score-label">SECURITY SCORE</div>
                    </div>
                </div>

                <div class="grid-2">
                    <div class="card">
                        <h2>Audit Target</h2>
                        <div class="detail-row"><span class="detail-label">Requested:</span><span class="detail-val">${escapeHtml(report.target.requestedUrl)}</span></div>
                        <div class="detail-row"><span class="detail-label">Final URL:</span><span class="detail-val">${escapeHtml(report.target.finalUrl)}</span></div>
                        <div class="detail-row"><span class="detail-label">Status Code:</span><span class="detail-val">${report.target.responseCode ?: "N/A"}</span></div>
                        <div class="detail-row"><span class="detail-label">Scan Date:</span><span class="detail-val">$formattedDate</span></div>
                        <div class="detail-row"><span class="detail-label">Response Time:</span><span class="detail-val">${String.format("%.2f", report.target.elapsedTimeSeconds)}s</span></div>
                        <div class="detail-row"><span class="detail-label">HTTPS Enabled:</span><span class="detail-val">${if (report.target.isHttps) "YES (Encrypted)" else "NO (Plaintext HTTP)"}</span></div>
                    </div>

                    <div class="card">
                        <h2>SSL/TLS Certificate</h2>
                        <div class="detail-row"><span class="detail-label">Subject:</span><span class="detail-val">${escapeHtml(report.sslInfo.subject)}</span></div>
                        <div class="detail-row"><span class="detail-label">Issuer:</span><span class="detail-val">${escapeHtml(report.sslInfo.issuer)}</span></div>
                        <div class="detail-row"><span class="detail-label">Protocol:</span><span class="detail-val">${escapeHtml(report.sslInfo.protocol)}</span></div>
                        <div class="detail-row"><span class="detail-label">Cipher Suite:</span><span class="detail-val" style="font-size:11px;">${escapeHtml(report.sslInfo.cipherSuite)}</span></div>
                        <div class="detail-row"><span class="detail-label">Expires in:</span><span class="detail-val">${report.sslInfo.daysRemaining} days</span></div>
                        <div class="detail-row"><span class="detail-label">Self-Signed:</span><span class="detail-val">${if (report.sslInfo.isSelfSigned) "YES (Warning)" else "NO (Trusted CA)"}</span></div>
                    </div>
                </div>

                <div class="card" style="margin-bottom: 24px;">
                    <h2>Executive Vulnerability Breakdown</h2>
                    <div style="display: flex; justify-content: space-around; text-align: center; padding: 12px 0;">
                        <div><div style="font-size: 24px; font-weight: bold; color: #EF4444;">${report.criticalCount}</div><div style="font-size: 11px; color: var(--text-muted);">CRITICAL</div></div>
                        <div><div style="font-size: 24px; font-weight: bold; color: #F97316;">${report.vulnerabilities.count { it.severity == SeverityLevel.HIGH }}</div><div style="font-size: 11px; color: var(--text-muted);">HIGH</div></div>
                        <div><div style="font-size: 24px; font-weight: bold; color: #F59E0B;">${report.warningCount}</div><div style="font-size: 11px; color: var(--text-muted);">MEDIUM / WARNING</div></div>
                        <div><div style="font-size: 24px; font-weight: bold; color: #3B82F6;">${report.vulnerabilities.count { it.severity == SeverityLevel.LOW }}</div><div style="font-size: 11px; color: var(--text-muted);">LOW</div></div>
                        <div><div style="font-size: 24px; font-weight: bold; color: #10B981;">${report.passedCount}</div><div style="font-size: 11px; color: var(--text-muted);">PASSED CHECKS</div></div>
                    </div>
                </div>

                <h2 style="color: var(--gold); border-bottom: 1px solid var(--border); padding-bottom: 8px;">Vulnerability Findings &amp; Remediation Guide</h2>
                $vulnerabilitiesHtml

                <div class="card" style="margin-top: 24px;">
                    <h2>Security Headers Audit</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>Header Name</th>
                                <th>Status</th>
                                <th>Policy &amp; Purpose</th>
                            </tr>
                        </thead>
                        <tbody>
                            $headersHtml
                        </tbody>
                    </table>
                </div>

                <div class="card" style="margin-top: 24px;">
                    <h2>Endpoint Recon &amp; Sensitive Paths</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>Endpoint</th>
                                <th>HTTP Response</th>
                                <th>Risk &amp; Notes</th>
                            </tr>
                        </thead>
                        <tbody>
                            $pathsHtml
                        </tbody>
                    </table>
                </div>

                <div class="footer">
                    SAMI Auditor • Authorized Defensive Security Research &amp; Penetration Testing Readiness • Generated: $formattedDate
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun buildJsonReport(report: AuditReport): String {
        val root = JSONObject()
        root.put("id", report.id)
        root.put("timestamp", report.timestamp)
        root.put("score", report.score)

        val target = JSONObject()
        target.put("requestedUrl", report.target.requestedUrl)
        target.put("finalUrl", report.target.finalUrl)
        target.put("statusCode", report.target.responseCode)
        target.put("elapsedSeconds", report.target.elapsedTimeSeconds)
        target.put("isHttps", report.target.isHttps)
        root.put("target", target)

        val ssl = JSONObject()
        ssl.put("isHttps", report.sslInfo.isHttps)
        ssl.put("subject", report.sslInfo.subject)
        ssl.put("issuer", report.sslInfo.issuer)
        ssl.put("validFrom", report.sslInfo.validFrom)
        ssl.put("validTo", report.sslInfo.validTo)
        ssl.put("daysRemaining", report.sslInfo.daysRemaining)
        ssl.put("protocol", report.sslInfo.protocol)
        ssl.put("cipherSuite", report.sslInfo.cipherSuite)
        ssl.put("isSelfSigned", report.sslInfo.isSelfSigned)
        root.put("sslCertificate", ssl)

        val vulnsArray = JSONArray()
        report.vulnerabilities.forEach { v ->
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("titleEn", v.titleEn)
            obj.put("titleAr", v.titleAr)
            obj.put("category", v.owaspCategory)
            obj.put("severity", v.severity.name)
            obj.put("cvssScore", v.cvssScore)
            obj.put("descriptionEn", v.descriptionEn)
            obj.put("descriptionAr", v.descriptionAr)
            obj.put("remediationEn", v.remediationEn)
            obj.put("remediationAr", v.remediationAr)
            obj.put("configSnippet", v.configSnippet ?: "")
            vulnsArray.put(obj)
        }
        root.put("vulnerabilities", vulnsArray)

        val headersArray = JSONArray()
        report.securityHeaders.forEach { h ->
            val obj = JSONObject()
            obj.put("name", h.name)
            obj.put("isPresent", h.isPresent)
            obj.put("value", h.rawValue ?: "")
            obj.put("descriptionEn", h.explanationEn)
            headersArray.put(obj)
        }
        root.put("securityHeaders", headersArray)

        val pathsArray = JSONArray()
        report.sensitivePaths.forEach { p ->
            val obj = JSONObject()
            obj.put("path", p.path)
            obj.put("statusCode", p.statusCode)
            obj.put("isAccessible", p.isAccessible)
            obj.put("risk", p.riskLevel.name)
            obj.put("description", p.description)
            pathsArray.put(obj)
        }
        root.put("sensitivePaths", pathsArray)

        return root.toString(2)
    }

    fun buildSummaryText(report: AuditReport): String {
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("          SAMI SECURITY AUDITOR REPORT            ")
        sb.appendLine("==================================================")
        sb.appendLine("Target: ${report.target.requestedUrl}")
        sb.appendLine("Final URL: ${report.target.finalUrl}")
        sb.appendLine("Score: ${report.score}/100")
        sb.appendLine("Critical: ${report.criticalCount} | Warnings: ${report.warningCount} | Passed: ${report.passedCount}")
        sb.appendLine()
        sb.appendLine("--- TOP VULNERABILITY FINDINGS ---")
        report.vulnerabilities.forEach { v ->
            sb.appendLine("[${v.severity.labelEn}] ${v.titleEn} (${v.owaspCategory})")
            sb.appendLine("    CVSS: ${v.cvssScore} • ${v.descriptionEn}")
            sb.appendLine("    Fix: ${v.remediationEn}")
        }
        sb.appendLine()
        sb.appendLine("--- SSL/TLS DETAILS ---")
        sb.appendLine("Protocol: ${report.sslInfo.protocol}")
        sb.appendLine("Issuer: ${report.sslInfo.issuer}")
        sb.appendLine("Days Remaining: ${report.sslInfo.daysRemaining}")
        sb.appendLine()
        sb.appendLine("--- SENSITIVE PATHS ---")
        report.sensitivePaths.forEach { p ->
            if (p.isAccessible) {
                sb.appendLine("[!] EXPOSED: /${p.path} (HTTP ${p.statusCode})")
            }
        }
        sb.appendLine("==================================================")
        sb.appendLine("Generated by SAMI Auditor")
        return sb.toString()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
