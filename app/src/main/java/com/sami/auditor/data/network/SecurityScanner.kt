package com.sami.auditor.data.network

import com.sami.auditor.data.model.AuditLogEntry
import com.sami.auditor.data.model.AuditReport
import com.sami.auditor.data.model.CookieFinding
import com.sami.auditor.data.model.LogSeverity
import com.sami.auditor.data.model.PathProbeResult
import com.sami.auditor.data.model.SecurityHeaderItem
import com.sami.auditor.data.model.TargetSummary
import com.sami.auditor.data.model.TechCorsFinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SecurityScanner {

    companion object {
        val SECURITY_HEADERS_DEF = listOf(
            Triple(
                "Content-Security-Policy",
                "تمنع هجمات XSS وحقن البرمجيات الخبيثة",
                "Prevents XSS and malicious script injection"
            ),
            Triple(
                "Strict-Transport-Security",
                "تفرض الاتصال المشفر عبر HTTPS (HSTS)",
                "Enforces encrypted HTTPS connections (HSTS)"
            ),
            Triple(
                "X-Frame-Options",
                "تحمي من هجمات Clickjacking",
                "Protects against UI redress & Clickjacking"
            ),
            Triple(
                "X-Content-Type-Options",
                "تمنع التخمين الخاطئ لأنواع الملفات (MIME Sniffing)",
                "Prevents MIME-type sniffing"
            ),
            Triple(
                "Referrer-Policy",
                "تحمي تسريب بيانات المرجع في الروابط",
                "Protects referrer leakage in requests"
            ),
            Triple(
                "Permissions-Policy",
                "تحكم الوصول للكاميرا والميكروفون والموقع",
                "Controls browser access to camera, mic & location"
            )
        )

        val SENSITIVE_PATHS = listOf(
            "admin",
            "login",
            ".env",
            "config.php",
            "robots.txt",
            ".git/HEAD",
            "sitemap.xml",
            "backup.sql"
        )

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) SAMI Auditor/2.0"
    }

    private val permissiveClient: OkHttpClient by lazy {
        createPermissiveClient(followRedirects = true)
    }

    private val noRedirectClient: OkHttpClient by lazy {
        createPermissiveClient(followRedirects = false)
    }

    private fun createPermissiveClient(followRedirects: Boolean): OkHttpClient {
        val trustAllCertificates = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCertificates, SecureRandom())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCertificates[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(followRedirects)
            .followSslRedirects(followRedirects)
            .build()
    }

    suspend fun audit(
        targetUrl: String,
        onLog: (AuditLogEntry) -> Unit
    ): AuditReport = withContext(Dispatchers.IO) {
        val rawLogs = mutableListOf<AuditLogEntry>()
        fun emitLog(text: String, severity: LogSeverity, bold: Boolean = false) {
            val entry = AuditLogEntry(text = text, severity = severity, isBold = bold)
            rawLogs.add(entry)
            onLog(entry)
        }

        var normalizedUrl = targetUrl.trim()
        if (!normalizedUrl.startsWith("http://", ignoreCase = true) &&
            !normalizedUrl.startsWith("https://", ignoreCase = true)
        ) {
            normalizedUrl = "https://$normalizedUrl"
        }

        val request = Request.Builder()
            .url(normalizedUrl)
            .header("User-Agent", USER_AGENT)
            .build()

        val startTime = System.currentTimeMillis()
        var mainResponse: Response? = null
        var connectionError: String? = null

        try {
            mainResponse = permissiveClient.newCall(request).execute()
        } catch (e: Exception) {
            connectionError = e.localizedMessage ?: e.message ?: "Connection failed"
        }

        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0

        if (mainResponse == null || connectionError != null) {
            emitLog("[-] Connection failed: $connectionError", LogSeverity.BAD, true)
            throw IOException(connectionError ?: "Connection failed")
        }

        val finalUrl = mainResponse.request.url.toString()
        val statusCode = mainResponse.code
        val isHttps = finalUrl.startsWith("https://", ignoreCase = true)
        val headers = mainResponse.headers

        // 1. Target Summary
        emitLog("=== TARGET SUMMARY ===", LogSeverity.HEADER, true)
        emitLog("URL: $finalUrl", LogSeverity.INFO)
        emitLog(
            "Status Code: $statusCode | Time: ${String.format("%.2f", elapsedSec)}s",
            LogSeverity.INFO
        )
        if (isHttps) {
            emitLog("[+] HTTPS Encryption Enabled", LogSeverity.GOOD)
        } else {
            emitLog("[-] HTTP Protocol Used (Insecure)", LogSeverity.BAD)
        }

        val targetSummary = TargetSummary(
            requestedUrl = normalizedUrl,
            finalUrl = finalUrl,
            responseCode = statusCode,
            elapsedTimeSeconds = elapsedSec,
            isHttps = isHttps
        )

        // 2. Security Headers Audit
        emitLog("\n=== HARDENING & HEADERS ===", LogSeverity.HEADER, true)
        val headerItems = mutableListOf<SecurityHeaderItem>()
        var passedHeaders = 0

        for ((name, arDesc, enDesc) in SECURITY_HEADERS_DEF) {
            val value = headers[name]
            val isPresent = !value.isNullOrBlank()
            if (isPresent) {
                passedHeaders++
                emitLog("[+] $name : PRESENT", LogSeverity.GOOD)
            } else {
                emitLog("[-] $name : MISSING", LogSeverity.BAD)
            }
            headerItems.add(
                SecurityHeaderItem(
                    name = name,
                    explanationAr = arDesc,
                    explanationEn = enDesc,
                    isPresent = isPresent,
                    rawValue = value
                )
            )
        }

        // 3. Information Disclosure & Technology Leak
        emitLog("\n=== TECH & CORS AUDIT ===", LogSeverity.HEADER, true)
        val server = headers["Server"]
        val poweredBy = headers["X-Powered-By"]
        val cors = headers["Access-Control-Allow-Origin"]

        if (!server.isNullOrBlank()) {
            emitLog("[!] Server Exposed: $server", LogSeverity.WARN)
        } else {
            emitLog("[+] Server Banner Hidden", LogSeverity.GOOD)
        }

        if (!poweredBy.isNullOrBlank()) {
            emitLog("[-] Technology Leak: $poweredBy", LogSeverity.BAD)
        }

        val isCorsWildcard = cors == "*"
        if (isCorsWildcard) {
            emitLog("[!] Wildcard CORS (*): High Exposure", LogSeverity.BAD)
        } else if (!cors.isNullOrBlank()) {
            emitLog("[+] CORS Restricted: $cors", LogSeverity.GOOD)
        } else {
            emitLog("[*] CORS Header Not Present", LogSeverity.INFO)
        }

        val techCorsFinding = TechCorsFinding(
            serverBanner = server,
            xPoweredBy = poweredBy,
            corsHeader = cors,
            isCorsWildcard = isCorsWildcard
        )

        // 4. Cookie Security Analysis
        emitLog("\n=== COOKIE FLAGS AUDIT ===", LogSeverity.HEADER, true)
        val cookieHeaders = headers.values("Set-Cookie")
        val cookieDetails = mutableListOf<String>()
        var httpOnlySet = false
        var secureSet = false
        var sameSiteConfigured = false

        if (cookieHeaders.isNotEmpty()) {
            val allCookiesLower = cookieHeaders.joinToString(" ; ").lowercase()

            httpOnlySet = allCookiesLower.contains("httponly")
            if (httpOnlySet) {
                emitLog("[+] Cookie Flag: HttpOnly Set", LogSeverity.GOOD)
            } else {
                emitLog("[-] Cookie Flag: HttpOnly Missing (XSS Risk)", LogSeverity.BAD)
            }

            secureSet = allCookiesLower.contains("secure")
            if (secureSet) {
                emitLog("[+] Cookie Flag: Secure Set", LogSeverity.GOOD)
            } else {
                emitLog("[-] Cookie Flag: Secure Missing", LogSeverity.BAD)
            }

            sameSiteConfigured = allCookiesLower.contains("samesite")
            if (sameSiteConfigured) {
                emitLog("[+] Cookie Flag: SameSite Configured", LogSeverity.GOOD)
            } else {
                emitLog("[!] Cookie Flag: SameSite Missing", LogSeverity.WARN)
            }

            cookieDetails.addAll(cookieHeaders)
        } else {
            emitLog("[*] No Set-Cookie headers detected", LogSeverity.INFO)
        }

        val cookieFinding = CookieFinding(
            hasCookies = cookieHeaders.isNotEmpty(),
            cookieCount = cookieHeaders.size,
            httpOnlySet = httpOnlySet,
            secureSet = secureSet,
            sameSiteConfigured = sameSiteConfigured,
            details = cookieDetails
        )

        // Close initial response body
        mainResponse.close()

        // 5. Parallel Sensitive Endpoints Recon
        emitLog("\n=== SENSITIVE PATHS CHECK ===", LogSeverity.HEADER, true)
        val baseUrlTrimmed = finalUrl.trimEnd('/')

        val pathResults = coroutineScope {
            SENSITIVE_PATHS.map { path ->
                async(Dispatchers.IO) {
                    checkEndpoint(baseUrlTrimmed, path)
                }
            }.awaitAll()
        }

        for (probe in pathResults) {
            when {
                probe.isAccessible -> {
                    emitLog("[!] [HTTP ${probe.statusCode}] /${probe.path} ACCESSIBLE", LogSeverity.BAD)
                }
                probe.statusCode != null -> {
                    emitLog("[+] [HTTP ${probe.statusCode}] /${probe.path}", LogSeverity.GOOD)
                }
                else -> {
                    emitLog("[-] [ERR] /${probe.path}", LogSeverity.INFO)
                }
            }
        }

        // Calculate score and counts
        var passedCount = 0
        var warningCount = 0
        var criticalCount = 0

        if (isHttps) passedCount++ else criticalCount++
        passedCount += passedHeaders
        criticalCount += (SECURITY_HEADERS_DEF.size - passedHeaders)

        if (server.isNullOrBlank()) passedCount++ else warningCount++
        if (poweredBy != null) criticalCount++ else passedCount++
        if (isCorsWildcard) criticalCount++ else if (cors != null) passedCount++

        if (cookieHeaders.isNotEmpty()) {
            if (httpOnlySet) passedCount++ else criticalCount++
            if (secureSet) passedCount++ else criticalCount++
            if (sameSiteConfigured) passedCount++ else warningCount++
        }

        for (probe in pathResults) {
            if (probe.isAccessible) {
                criticalCount++
            } else if (probe.statusCode != null) {
                passedCount++
            }
        }

        val totalChecks = passedCount + warningCount + criticalCount
        val score = if (totalChecks > 0) {
            val raw = ((passedCount.toDouble() * 1.0 + warningCount.toDouble() * 0.4) / totalChecks.toDouble()) * 100.0
            raw.toInt().coerceIn(0, 100)
        } else {
            0
        }

        emitLog("\n=== AUDIT COMPLETED ===", LogSeverity.HEADER, true)
        emitLog("Security Score: $score/100 | Passed: $passedCount | Warnings: $warningCount | Critical: $criticalCount", LogSeverity.GOOD)

        AuditReport(
            target = targetSummary,
            securityHeaders = headerItems,
            techCors = techCorsFinding,
            cookies = cookieFinding,
            sensitivePaths = pathResults,
            rawLogs = rawLogs,
            score = score,
            passedCount = passedCount,
            warningCount = warningCount,
            criticalCount = criticalCount
        )
    }

    private fun checkEndpoint(baseUrl: String, path: String): PathProbeResult {
        val targetUrl = "$baseUrl/$path"
        val request = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", USER_AGENT)
            .build()

        return try {
            noRedirectClient.newCall(request).execute().use { response ->
                val code = response.code
                PathProbeResult(
                    path = path,
                    statusCode = code,
                    isAccessible = code in 200..399,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            PathProbeResult(
                path = path,
                statusCode = null,
                isAccessible = false,
                errorMessage = e.message
            )
        }
    }
}
