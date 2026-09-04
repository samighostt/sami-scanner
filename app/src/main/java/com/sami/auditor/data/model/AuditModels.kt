package com.sami.auditor.data.model

enum class ScanStatus {
    IDLE,
    SCANNING,
    COMPLETED,
    FAILED
}

enum class LogSeverity {
    HEADER,
    GOOD,
    BAD,
    WARN,
    INFO
}

data class AuditLogEntry(
    val id: Long = System.nanoTime(),
    val text: String,
    val severity: LogSeverity = LogSeverity.INFO,
    val isBold: Boolean = false
)

data class TargetSummary(
    val requestedUrl: String = "",
    val finalUrl: String = "",
    val responseCode: Int? = null,
    val elapsedTimeSeconds: Double = 0.0,
    val isHttps: Boolean = false
)

data class SecurityHeaderItem(
    val name: String,
    val explanationAr: String,
    val explanationEn: String,
    val isPresent: Boolean,
    val rawValue: String? = null
)

data class TechCorsFinding(
    val serverBanner: String? = null,
    val xPoweredBy: String? = null,
    val corsHeader: String? = null,
    val isCorsWildcard: Boolean = false
)

data class CookieFinding(
    val hasCookies: Boolean = false,
    val cookieCount: Int = 0,
    val httpOnlySet: Boolean = false,
    val secureSet: Boolean = false,
    val sameSiteConfigured: Boolean = false,
    val details: List<String> = emptyList()
)

data class PathProbeResult(
    val path: String,
    val statusCode: Int?,
    val isAccessible: Boolean, // code < 400
    val errorMessage: String? = null
)

data class AuditReport(
    val target: TargetSummary = TargetSummary(),
    val securityHeaders: List<SecurityHeaderItem> = emptyList(),
    val techCors: TechCorsFinding = TechCorsFinding(),
    val cookies: CookieFinding = CookieFinding(),
    val sensitivePaths: List<PathProbeResult> = emptyList(),
    val rawLogs: List<AuditLogEntry> = emptyList(),
    val score: Int = 0,
    val passedCount: Int = 0,
    val warningCount: Int = 0,
    val criticalCount: Int = 0
)
