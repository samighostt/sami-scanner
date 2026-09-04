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

enum class SeverityLevel(val labelEn: String, val labelAr: String, val scoreWeight: Int) {
    CRITICAL("CRITICAL", "حرج", 10),
    HIGH("HIGH", "عالي", 7),
    MEDIUM("MEDIUM", "متوسط", 4),
    LOW("LOW", "منخفض", 2),
    INFO("INFO", "معلوماتي", 0),
    PASSED("SECURE", "آمن", 0)
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
    val ipAddress: String? = null,
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
    val riskLevel: SeverityLevel = SeverityLevel.INFO,
    val description: String = "",
    val errorMessage: String? = null
)

data class SslCertificateInfo(
    val isHttps: Boolean = false,
    val subject: String = "N/A",
    val issuer: String = "N/A",
    val validFrom: String = "N/A",
    val validTo: String = "N/A",
    val daysRemaining: Long = 0,
    val protocol: String = "N/A",
    val cipherSuite: String = "N/A",
    val isExpired: Boolean = false,
    val isExpiringSoon: Boolean = false,
    val isSelfSigned: Boolean = false
)

data class RateLimitFinding(
    val isProtectionDetected: Boolean = false,
    val limitHeader: String? = null,
    val remainingHeader: String? = null,
    val retryAfterHeader: String? = null,
    val summaryAr: String = "",
    val summaryEn: String = ""
)

data class VulnerabilityFinding(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val owaspCategory: String, // e.g. "A05: Security Misconfiguration"
    val severity: SeverityLevel,
    val cvssScore: Double,
    val descriptionAr: String,
    val descriptionEn: String,
    val remediationAr: String,
    val remediationEn: String,
    val configSnippet: String? = null
)

data class AuditReport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val target: TargetSummary = TargetSummary(),
    val securityHeaders: List<SecurityHeaderItem> = emptyList(),
    val techCors: TechCorsFinding = TechCorsFinding(),
    val cookies: CookieFinding = CookieFinding(),
    val sensitivePaths: List<PathProbeResult> = emptyList(),
    val sslInfo: SslCertificateInfo = SslCertificateInfo(),
    val rateLimit: RateLimitFinding = RateLimitFinding(),
    val vulnerabilities: List<VulnerabilityFinding> = emptyList(),
    val rawLogs: List<AuditLogEntry> = emptyList(),
    val score: Int = 0,
    val passedCount: Int = 0,
    val warningCount: Int = 0,
    val criticalCount: Int = 0
)

data class AuditHistoryItem(
    val id: String,
    val url: String,
    val timestamp: Long,
    val score: Int,
    val criticalCount: Int,
    val warningCount: Int,
    val passedCount: Int
)

data class MonitoredSite(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String,
    val intervalHours: Int = 24, // 24 = Daily, 168 = Weekly
    val lastChecked: Long = 0L,
    val lastScore: Int = 0,
    val status: String = "PENDING", // "SECURE", "WARNING", "CRITICAL", "PENDING"
    val isActive: Boolean = true
)
