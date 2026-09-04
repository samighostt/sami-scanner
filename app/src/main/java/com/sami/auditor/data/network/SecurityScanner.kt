package com.sami.auditor.data.network

import com.sami.auditor.data.model.AuditLogEntry
import com.sami.auditor.data.model.AuditReport
import com.sami.auditor.data.model.CookieFinding
import com.sami.auditor.data.model.LogSeverity
import com.sami.auditor.data.model.PathProbeResult
import com.sami.auditor.data.model.RateLimitFinding
import com.sami.auditor.data.model.SecurityHeaderItem
import com.sami.auditor.data.model.SeverityLevel
import com.sami.auditor.data.model.SslCertificateInfo
import com.sami.auditor.data.model.TargetSummary
import com.sami.auditor.data.model.TechCorsFinding
import com.sami.auditor.data.model.VulnerabilityFinding
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SecurityScanner {

    companion object {
        val SECURITY_HEADERS_DEF = listOf(
            Triple(
                "Content-Security-Policy",
                "تمنع هجمات XSS وحقن البرمجيات الخبيثة وتحدد مصادر السكربتات الموثوقة",
                "Prevents XSS, data injection, and restricts authorized script sources"
            ),
            Triple(
                "Strict-Transport-Security",
                "تفرض الاتصال المشفر عبر HTTPS وتمنع هجمات خفض التشفير (HSTS)",
                "Enforces encrypted HTTPS connections & prevents downgrade attacks (HSTS)"
            ),
            Triple(
                "X-Frame-Options",
                "تحمي من هجمات التضمين الخبيث وخطف النقرات (Clickjacking)",
                "Protects against UI redress & Clickjacking embedding"
            ),
            Triple(
                "X-Content-Type-Options",
                "تمنع المتصفح من تخمين أنواع الملفات المرفوعة (nosniff)",
                "Prevents MIME-type sniffing by enforcing declared content-type"
            ),
            Triple(
                "Referrer-Policy",
                "تحمي تسريب بيانات الروابط والمرجع إلى مواقع خارجية",
                "Controls how much referrer information is sent with requests"
            ),
            Triple(
                "Permissions-Policy",
                "تتحكم في وصول المتصفح للكاميرا والميكروفون وتحديد الموقع",
                "Restricts browser access to sensitive features like camera, mic & geolocation"
            )
        )

        val SENSITIVE_PROBES = listOf(
            Triple(".env", "Environment configuration file (Contains API keys/passwords)", SeverityLevel.CRITICAL),
            Triple(".git/HEAD", "Git source code repository metadata", SeverityLevel.CRITICAL),
            Triple("backup.sql", "Database raw backup dump", SeverityLevel.CRITICAL),
            Triple("backup.zip", "Full website archive backup", SeverityLevel.HIGH),
            Triple("config.php", "Server script configuration file", SeverityLevel.HIGH),
            Triple("admin", "Administrative management dashboard", SeverityLevel.MEDIUM),
            Triple("wp-admin", "WordPress CMS administration panel", SeverityLevel.MEDIUM),
            Triple("phpmyadmin", "Web-based MySQL database administrator", SeverityLevel.HIGH),
            Triple(".htaccess", "Apache web server configuration override file", SeverityLevel.HIGH),
            Triple("robots.txt", "Web crawler exclusion rules & exposed paths", SeverityLevel.INFO),
            Triple("sitemap.xml", "Search engine index of public endpoints", SeverityLevel.INFO),
            Triple(".well-known/security.txt", "Security vulnerability disclosure standard policy", SeverityLevel.INFO)
        )

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 SAMI-Auditor/3.0"
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
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(followRedirects)
            .followSslRedirects(followRedirects)
            .build()
    }

    suspend fun audit(
        targetUrl: String,
        onLog: (AuditLogEntry) -> Unit
    ): AuditReport = withContext(Dispatchers.IO) {
        val rawLogs = mutableListOf<AuditLogEntry>()
        val vulnerabilities = mutableListOf<VulnerabilityFinding>()

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

        emitLog("=== INITIATING COMPREHENSIVE SECURITY AUDIT ===", LogSeverity.HEADER, true)
        emitLog("Target: $normalizedUrl", LogSeverity.INFO)
        emitLog("Standard: OWASP Top 10 • CVSS v3.1 Scoring • TLS Inspection", LogSeverity.INFO)

        val request = Request.Builder()
            .url(normalizedUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
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
            emitLog("[-] Target Unreachable: $connectionError", LogSeverity.BAD, true)
            throw IOException(connectionError ?: "Connection failed")
        }

        val finalUrl = mainResponse.request.url.toString()
        val statusCode = mainResponse.code
        val isHttps = finalUrl.startsWith("https://", ignoreCase = true)
        val headers = mainResponse.headers
        val responseBodyString = try {
            mainResponse.peekBody(1024 * 64).string()
        } catch (e: Exception) {
            ""
        }

        emitLog("\n[+] Target connected in ${String.format("%.2f", elapsedSec)}s | Status HTTP $statusCode", LogSeverity.GOOD)

        // 1. SSL/TLS Certificate Analysis
        emitLog("\n=== SSL/TLS CERTIFICATE AUDIT ===", LogSeverity.HEADER, true)
        val sslInfo = extractSslInfo(mainResponse, isHttps)
        if (!isHttps) {
            emitLog("[-] CRITICAL: Insecure HTTP Protocol Used (No Encryption)", LogSeverity.BAD, true)
            vulnerabilities.add(
                VulnerabilityFinding(
                    id = "OWASP-A02-HTTP",
                    titleEn = "Unencrypted HTTP Communication",
                    titleAr = "استخدام بروتوكول HTTP غير المشفر",
                    owaspCategory = "A02: Cryptographic Failures",
                    severity = SeverityLevel.CRITICAL,
                    cvssScore = 9.1,
                    descriptionEn = "Target does not enforce HTTPS. Data transmitted over this connection (passwords, tokens, personal info) is vulnerable to Man-in-the-Middle (MITM) eavesdropping.",
                    descriptionAr = "الموقع يستخدم اتصالاً غير مشفر مما يعرض بيانات المستخدمين وكلمات المرور لاعتراض المتسللين عبر هجمات رجل في المنتصف (MITM).",
                    remediationEn = "Configure an SSL/TLS certificate (e.g. Let's Encrypt) and redirect all HTTP traffic to HTTPS with a 301 Permanent Redirect.",
                    remediationAr = "قم بتركيب شهادة SSL/TLS وإجبار تحويل كافة الزوار من HTTP إلى HTTPS تلقائياً.",
                    configSnippet = """
                        # Nginx Configuration
                        server {
                            listen 80;
                            server_name yourdomain.com;
                            return 301 https://${'$'}host${'$'}request_uri;
                        }
                    """.trimIndent()
                )
            )
        } else {
            emitLog("[+] TLS Protocol: ${sslInfo.protocol}", LogSeverity.GOOD)
            emitLog("[+] Certificate Issuer: ${sslInfo.issuer}", LogSeverity.INFO)
            emitLog("[+] Cipher Suite: ${sslInfo.cipherSuite}", LogSeverity.INFO)
            emitLog("[+] Validity: ${sslInfo.daysRemaining} days remaining", if (sslInfo.daysRemaining < 30) LogSeverity.WARN else LogSeverity.GOOD)

            if (sslInfo.isExpired) {
                emitLog("[-] Certificate EXPIRED!", LogSeverity.BAD, true)
                vulnerabilities.add(
                    VulnerabilityFinding(
                        id = "SSL-EXPIRED",
                        titleEn = "Expired SSL/TLS Certificate",
                        titleAr = "شهادة التشفير SSL منتهية الصلاحية",
                        owaspCategory = "A02: Cryptographic Failures",
                        severity = SeverityLevel.HIGH,
                        cvssScore = 7.5,
                        descriptionEn = "The SSL/TLS certificate has expired. Browsers will display scary security warnings and block users.",
                        descriptionAr = "شهادة الأمان منتهية الصلاحية وستقوم المتصفحات بحظر دخول الزوار وإظهار تنبيهات تحذيرية.",
                        remediationEn = "Renew the SSL certificate immediately through your certificate authority or certbot.",
                        remediationAr = "قم بتجديد شهادة الأمان فوراً عبر Certbot أو مزود الخدمة الخاص بك.",
                        configSnippet = "certbot renew --force-renewal"
                    )
                )
            } else if (sslInfo.isExpiringSoon) {
                emitLog("[!] Certificate expires in less than 30 days", LogSeverity.WARN)
                vulnerabilities.add(
                    VulnerabilityFinding(
                        id = "SSL-EXPIRING-SOON",
                        titleEn = "SSL Certificate Expiring Soon",
                        titleAr = "شهادة الأمان على وشك الانتهاء",
                        owaspCategory = "A02: Cryptographic Failures",
                        severity = SeverityLevel.LOW,
                        cvssScore = 3.3,
                        descriptionEn = "The certificate will expire in ${sslInfo.daysRemaining} days. Setup automated renewal.",
                        descriptionAr = "تنتهي صلاحية الشهادة بعد ${sslInfo.daysRemaining} يوماً. يجب التحقق من تفعيل التجديد التلقائي.",
                        remediationEn = "Verify automated cronjob or certbot renewal daemon is running.",
                        remediationAr = "تأكد من عمل مهمة التجديد التلقائي للشهادة لمنع انقطاع الموقع.",
                        configSnippet = "sudo certbot renew --dry-run"
                    )
                )
            }

            if (sslInfo.isSelfSigned) {
                emitLog("[!] Self-Signed Certificate Detected", LogSeverity.WARN)
            }
        }

        // 2. Security Headers Analysis (OWASP A05)
        emitLog("\n=== HTTP SECURITY HEADERS AUDIT ===", LogSeverity.HEADER, true)
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

        // Add vulnerability findings for missing critical headers
        if (headers["Content-Security-Policy"] == null) {
            vulnerabilities.add(
                VulnerabilityFinding(
                    id = "OWASP-A03-CSP",
                    titleEn = "Missing Content-Security-Policy (CSP)",
                    titleAr = "غياب رأس سياسة أمان المحتوى (CSP)",
                    owaspCategory = "A05: Security Misconfiguration",
                    severity = SeverityLevel.HIGH,
                    cvssScore = 7.2,
                    descriptionEn = "Absence of CSP allows attackers to execute unauthorized JavaScript, inline styles, and exfiltrate credentials via Cross-Site Scripting (XSS).",
                    descriptionAr = "عدم وجود رأس CSP يجعل الموقع عرضة لتنفيذ هجمات البرمجة عبر المواقع (XSS) وحقن برمجيات خبيثة داخل صفحات المستخدمين.",
                    remediationEn = "Define a strict Content-Security-Policy restricting script sources to 'self' and trusted CDNs.",
                    remediationAr = "قم بإضافة رأس Content-Security-Policy لتحديد النطاقات المسموح بتحميل السكربتات منها فقط.",
                    configSnippet = """
                        # Nginx Security Header
                        add_header Content-Security-Policy "default-src 'self'; script-src 'self' https://trustedscripts.com; object-src 'none';" always;
                    """.trimIndent()
                )
            )
        }

        if (isHttps && headers["Strict-Transport-Security"] == null) {
            vulnerabilities.add(
                VulnerabilityFinding(
                    id = "OWASP-A05-HSTS",
                    titleEn = "Missing Strict-Transport-Security (HSTS)",
                    titleAr = "غياب رأس فرض التشفير الصارم (HSTS)",
                    owaspCategory = "A05: Security Misconfiguration",
                    severity = SeverityLevel.MEDIUM,
                    cvssScore = 5.3,
                    descriptionEn = "Without HSTS, attackers on the same network can intercept initial connections and strip SSL using SSL-Strip attacks.",
                    descriptionAr = "غياب HSTS يسمح للمهاجمين على نفس الشبكة بتجريد التشفير والنزول إلى HTTP غير المشفر عبر هجمات SSL-Strip.",
                    remediationEn = "Enable HSTS header with at least 1 year (31536000 seconds) duration and includeSubDomains.",
                    remediationAr = "قم بتفعيل رأس Strict-Transport-Security لفرض اتصال HTTPS الإجباري لمدة سنة على الأقل.",
                    configSnippet = """
                        # Nginx HSTS Header
                        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
                    """.trimIndent()
                )
            )
        }

        if (headers["X-Frame-Options"] == null && (headers["Content-Security-Policy"] == null || !headers["Content-Security-Policy"]!!.contains("frame-ancestors"))) {
            vulnerabilities.add(
                VulnerabilityFinding(
                    id = "OWASP-A04-CLICKJACKING",
                    titleEn = "Clickjacking Risk (Missing X-Frame-Options)",
                    titleAr = "قابلية للاختطاف عبر الإطارات (Clickjacking)",
                    owaspCategory = "A04: Insecure Design",
                    severity = SeverityLevel.MEDIUM,
                    cvssScore = 5.4,
                    descriptionEn = "The application does not restrict framing via iframes, allowing malicious sites to overlay invisible click targets to deceive users.",
                    descriptionAr = "يمكن للمواقع الخبيثة تضمين صفحات موقعك داخل إطارات خفية لخداع المستخدمين للنقر على أزرار حساسة دون علمهم.",
                    remediationEn = "Add 'X-Frame-Options: SAMEORIGIN' or 'frame-ancestors 'self'' in CSP.",
                    remediationAr = "أضف رأس X-Frame-Options بقيمة SAMEORIGIN لمنع تضمين موقعك في مواقع خارجية.",
                    configSnippet = """
                        # Nginx Frame Protection
                        add_header X-Frame-Options "SAMEORIGIN" always;
                    """.trimIndent()
                )
            )
        }

        if (headers["X-Content-Type-Options"] == null) {
            vulnerabilities.add(
                VulnerabilityFinding(
                    id = "OWASP-A05-MIME",
                    titleEn = "MIME Sniffing Vulnerability",
                    titleAr = "قابلية تخمين نوع المحتوى (MIME Sniffing)",
                    owaspCategory = "A05: Security Misconfiguration",
                    severity = SeverityLevel.LOW,
                    cvssScore = 3.5,
                    descriptionEn = "Browsers may attempt to guess the content type of uploaded files, potentially executing disguised HTML/JS in image files.",
                    descriptionAr = "المتصفح قد يحاول تخمين نوع الملفات المرفوعة مما قد يؤدي لتشغيل أكواد برمجية خبيثة متنكرة في صور.",
                    remediationEn = "Enforce 'X-Content-Type-Options: nosniff'.",
                    remediationAr = "أضف رأس X-Content-Type-Options بقيمة nosniff لفرض الالتزام بنوع الملف المصرح به فقط.",
                    configSnippet = "add_header X-Content-Type-Options \"nosniff\" always;"
                )
            )
        }

        // 3. Information Disclosure & Technology Leaks
        emitLog("\n=== TECH & INFORMATION DISCLOSURE ===", LogSeverity.HEADER, true)
        val serverBanner = headers["Server"]
        val xPoweredBy = headers["X-Powered-By"]
        val corsHeader = headers["Access-Control-Allow-Origin"]
        val isCorsWildcard = corsHeader == "*"

        if (!serverBanner.isNullOrBlank()) {
            emitLog("[!] Server Banner Exposed: $serverBanner", LogSeverity.WARN)
            vulnerabilities.add(
                VulnerabilityFinding(
                    id = "INFO-SERVER-BANNER",
                    titleEn = "Web Server Version Banner Disclosure",
                    titleAr = "كشف نوع وإصدار خادم الويب (Server Header)",
                    owaspCategory = "A05: Security Misconfiguration",
                    severity = SeverityLevel.LOW,
                    cvssScore = 3.1,
                    descriptionEn = "The Server response header discloses the web server software ($serverBanner), aiding attackers in fingerprinting known CVE exploits.",
                    descriptionAr = "الخادم يكشف عن نوعه وإصداره في الترويسات مما يسهل على المهاجمين استهداف الثغرات المعروفة لهذا الإصدار.",
                    remediationEn = "Hide server tokens in web server configuration.",
                    remediationAr = "قم بإخفاء إصدار الخادم في ملف إعدادات Nginx أو Apache.",
                    configSnippet = """
                        # Nginx
                        server_tokens off;
                        
                        # Apache
                        ServerSignature Off
                        ServerTokens Prod
                    """.trimIndent()
                )
            )
        }

        if (!xPoweredBy.isNullOrBlank()) {
            emitLog("[-] Technology Leak: $xPoweredBy", LogSeverity.BAD)
            vulnerabilities.add(
                VulnerabilityFinding(
                    id = "INFO-POWERED-BY",
                    titleEn = "Backend Framework Disclosure (X-Powered-By)",
                    titleAr = "كشف لغة وإطار العمل البرمجي (X-Powered-By)",
                    owaspCategory = "A05: Security Misconfiguration",
                    severity = SeverityLevel.LOW,
                    cvssScore = 3.2,
                    descriptionEn = "The header 'X-Powered-By: $xPoweredBy' reveals the underlying programming language or framework.",
                    descriptionAr = "رأس X-Powered-By يفصح عن لغة البرمجة أو إطار العمل المستعمل ($xPoweredBy).",
                    remediationEn = "Disable the X-Powered-By header in your backend application or reverse proxy.",
                    remediationAr = "قم بإيقاف إرسال هذا الرأس في إعدادات PHP أو Express.js أو Nginx.",
                    configSnippet = """
                        # PHP (php.ini)
                        expose_php = Off
                        
                        # Express.js (Node.js)
                        app.disable('x-powered-by');
                    """.trimIndent()
                )
            )
        }

        if (isCorsWildcard) {
            emitLog("[-] Wildcard CORS Detected (*)", LogSeverity.BAD)
            vulnerabilities.add(
                VulnerabilityFinding(
                    id = "OWASP-A01-CORS",
                    titleEn = "Overly Permissive CORS Policy (Wildcard '*')",
                    titleAr = "سياسة CORS مفرطة التساهل (نجمة عامة *)",
                    owaspCategory = "A01: Broken Access Control",
                    severity = SeverityLevel.MEDIUM,
                    cvssScore = 6.5,
                    descriptionEn = "The server specifies Access-Control-Allow-Origin: *, allowing any third-party domain to read API responses.",
                    descriptionAr = "الخادم يسمح لأي موقع خارجي بقراءة استجابات الواجهات البرمجية وتجاوز سياسة المنشأ المشترك (SOP).",
                    remediationEn = "Explicitly whitelist authorized frontend origins instead of using '*'.",
                    remediationAr = "حدد بدقة النطاقات المسموح لها بالوصول بدلاً من السماح للجميع (*).",
                    configSnippet = "add_header Access-Control-Allow-Origin \"https://app.yourdomain.com\";"
                )
            )
        }

        // 4. Cookie Hygiene & Session Security (OWASP A07)
        emitLog("\n=== COOKIE SECURITY AUDIT ===", LogSeverity.HEADER, true)
        val cookieHeaders = headers.values("Set-Cookie")
        var httpOnlySet = false
        var secureSet = false
        var sameSiteConfigured = false

        if (cookieHeaders.isNotEmpty()) {
            val allCookiesLower = cookieHeaders.joinToString(" ; ").lowercase()
            httpOnlySet = allCookiesLower.contains("httponly")
            secureSet = allCookiesLower.contains("secure")
            sameSiteConfigured = allCookiesLower.contains("samesite")

            if (!httpOnlySet) {
                emitLog("[-] Cookie Flag: HttpOnly Missing (XSS Session Hijacking Risk)", LogSeverity.BAD)
                vulnerabilities.add(
                    VulnerabilityFinding(
                        id = "OWASP-A07-COOKIE-HTTPONLY",
                        titleEn = "Session Cookies Lack HttpOnly Flag",
                        titleAr = "ملفات تعريف الارتباط تفتقر لخاصية HttpOnly",
                        owaspCategory = "A07: Identification and Authentication Failures",
                        severity = SeverityLevel.HIGH,
                        cvssScore = 7.4,
                        descriptionEn = "Cookies without the HttpOnly attribute can be read by JavaScript, allowing attackers to steal session tokens via XSS.",
                        descriptionAr = "عدم تعيين خاصية HttpOnly يمكن سكربتات جافاسكربت من قراءة كوكيز الجلسة وسرقتها في حال وجود ثغرة XSS.",
                        remediationEn = "Configure your session manager to enforce HttpOnly on all authentication cookies.",
                        remediationAr = "قم بتفعيل خاصية HttpOnly لجميع ملفات الجلسات الحساسة.",
                        configSnippet = "Set-Cookie: session_id=abc123; Path=/; Secure; HttpOnly; SameSite=Lax"
                    )
                )
            } else {
                emitLog("[+] Cookie Flag: HttpOnly Set", LogSeverity.GOOD)
            }

            if (!secureSet && isHttps) {
                emitLog("[-] Cookie Flag: Secure Missing", LogSeverity.BAD)
                vulnerabilities.add(
                    VulnerabilityFinding(
                        id = "OWASP-A02-COOKIE-SECURE",
                        titleEn = "Cookies Lack Secure Attribute",
                        titleAr = "ملفات الكوكيز تفتقر لخاصية Secure",
                        owaspCategory = "A02: Cryptographic Failures",
                        severity = SeverityLevel.MEDIUM,
                        cvssScore = 5.2,
                        descriptionEn = "Cookies without the Secure attribute may be transmitted over unencrypted HTTP channels.",
                        descriptionAr = "قد يتم إرسال الكوكيز عبر قنوات غير مشفرة إذا لم يتم تفعيل خاصية Secure.",
                        remediationEn = "Always set the 'Secure' flag for all cookies on HTTPS sites.",
                        remediationAr = "فعل وسم Secure لضمان عدم إرسال الكوكيز إلا عبر اتصال HTTPS مشفر.",
                        configSnippet = "Set-Cookie: token=xyz; Secure; SameSite=Strict"
                    )
                )
            } else if (secureSet) {
                emitLog("[+] Cookie Flag: Secure Set", LogSeverity.GOOD)
            }

            if (!sameSiteConfigured) {
                emitLog("[!] Cookie Flag: SameSite Missing (CSRF Risk)", LogSeverity.WARN)
                vulnerabilities.add(
                    VulnerabilityFinding(
                        id = "OWASP-A01-COOKIE-SAMESITE",
                        titleEn = "Missing SameSite Cookie Protection (CSRF Vulnerability)",
                        titleAr = "غياب وسم SameSite في الكوكيز (ثغرة CSRF)",
                        owaspCategory = "A01: Broken Access Control",
                        severity = SeverityLevel.MEDIUM,
                        cvssScore = 5.9,
                        descriptionEn = "Without SameSite=Lax or SameSite=Strict, browsers include cookies in cross-site requests, leaving the site vulnerable to Cross-Site Request Forgery (CSRF).",
                        descriptionAr = "غياب وسم SameSite يسمح بإرسال الكوكيز مع الطلبات القادمة من مواقع أخرى مما يعرض المستخدمين لهجمات تزوير الطلبات (CSRF).",
                        remediationEn = "Set SameSite=Lax or SameSite=Strict on all state-changing cookies.",
                        remediationAr = "عين خاصية SameSite=Lax أو SameSite=Strict لجميع كوكيز المصادقة.",
                        configSnippet = "Set-Cookie: user_auth=secret; SameSite=Lax; Secure; HttpOnly"
                    )
                )
            } else {
                emitLog("[+] Cookie Flag: SameSite Configured", LogSeverity.GOOD)
            }
        } else {
            emitLog("[*] No Set-Cookie headers on root endpoint", LogSeverity.INFO)
        }

        // 5. Rate Limiting & Brute Force Protection Audit (فحص الحماية من هجمات التخمين)
        emitLog("\n=== RATE LIMITING & BRUTE FORCE RESISTANCE AUDIT ===", LogSeverity.HEADER, true)
        val rateLimitResult = evaluateRateLimiting(headers)
        if (!rateLimitResult.isProtectionDetected) {
            emitLog("[!] No Rate Limiting Headers Detected (High Brute Force Exposure)", LogSeverity.WARN)
            vulnerabilities.add(
                VulnerabilityFinding(
                    id = "BRUTE-FORCE-NO-RATE-LIMIT",
                    titleEn = "Lack of Rate Limiting Headers (Brute Force Exposure)",
                    titleAr = "غياب مؤشرات تحديد معدل الطلبات (قابلية لهجمات التخمين)",
                    owaspCategory = "A07: Identification and Authentication Failures",
                    severity = SeverityLevel.MEDIUM,
                    cvssScore = 5.8,
                    descriptionEn = "No Rate-Limiting or Request Throttling headers (X-RateLimit-*, Retry-After) were returned. The server may be vulnerable to automated brute-force attacks against login and authentication portals.",
                    descriptionAr = "لم يتم رصد رؤوس تحديد معدل الطلبات (Rate Limiting). قد يكون الخادم وواجهات تسجيل الدخول عرضة لمحاولات التخمين المتكررة والقوة الغاشمة دون حظر تلقائي.",
                    remediationEn = "Implement an application-level rate limiter or reverse proxy limit (e.g., Nginx limit_req or Cloudflare Rate Limiting) to restrict excessive requests.",
                    remediationAr = "قم بتفعيل تحديد معدل الطلبات عبر Nginx أو Fail2Ban أو جدار حماية سحابي (WAF) للحد من المحاولات المتكررة.",
                    configSnippet = """
                        # Nginx Rate Limiting
                        limit_req_zone ${'$'}binary_remote_addr zone=login_limit:10m rate=5r/m;
                        
                        location /login {
                            limit_req zone=login_limit burst=10 nodelay;
                        }
                    """.trimIndent()
                )
            )
        } else {
            emitLog("[+] Rate Limiting Protection Active: ${rateLimitResult.summaryEn}", LogSeverity.GOOD)
        }

        // 6. SQL Injection & Error Stack Disclosures (OWASP A03)
        emitLog("\n=== SQL INJECTION & ERROR DISCLOSURE AUDIT ===", LogSeverity.HEADER, true)
        val sqlErrorSignatures = listOf(
            "SQL syntax", "mysql_fetch", "ORA-01756", "PostgreSQL query failed",
            "SQLite/JDBCDriverException", "Microsoft OLE DB Provider for SQL Server",
            "Unclosed quotation mark", "Warning: mysql_", "Fatal error: Uncaught PDOException"
        )
        val detectedSqlError = sqlErrorSignatures.firstOrNull { responseBodyString.contains(it, ignoreCase = true) }
        if (detectedSqlError != null) {
            emitLog("[-] CRITICAL: Database Error Stack Trace Detected: $detectedSqlError", LogSeverity.BAD, true)
            vulnerabilities.add(
                VulnerabilityFinding(
                    id = "OWASP-A03-SQLI-STACK",
                    titleEn = "Database Error / SQL Stack Trace Disclosure",
                    titleAr = "كشف أخطاء قواعد البيانات (مؤشر ثغرة حقن SQL)",
                    owaspCategory = "A03: Injection",
                    severity = SeverityLevel.CRITICAL,
                    cvssScore = 8.9,
                    descriptionEn = "The application discloses detailed SQL engine exceptions ($detectedSqlError), confirming improper error handling and high probability of SQL Injection.",
                    descriptionAr = "يعرض التطبيق رسائل أخطاء قواعد البيانات التفصيلية ($detectedSqlError)، مما يؤكد غياب المعالجة السليمة للأخطاء وقابلية استغلال حقن SQL.",
                    remediationEn = "Disable detailed debug errors in production and strictly use parameterized queries (Prepared Statements).",
                    remediationAr = "عطّل عرض رسائل الأخطاء للعامة واستخدم الاستعلامات المجهزة (Parameterized Queries) حصراً.",
                    configSnippet = """
                        // Safe Prepared Statement (Java/Kotlin/PHP)
                        val stmt = connection.prepareStatement("SELECT * FROM users WHERE id = ?")
                        stmt.setString(1, userId)
                        val rs = stmt.executeQuery()
                    """.trimIndent()
                )
            )
        } else {
            emitLog("[+] No Database Stack Traces Leaked in Main Response", LogSeverity.GOOD)
        }

        // Close initial response body
        mainResponse.close()

        // 7. Directory Fuzzing & Endpoint Discovery (OWASP A01 & A05)
        emitLog("\n=== SENSITIVE PATHS & DIRECTORY FUZZING ===", LogSeverity.HEADER, true)
        val baseUrlTrimmed = finalUrl.trimEnd('/')

        val pathResults = coroutineScope {
            SENSITIVE_PROBES.map { (path, description, risk) ->
                async(Dispatchers.IO) {
                    probeEndpoint(baseUrlTrimmed, path, description, risk)
                }
            }.awaitAll()
        }

        for (probe in pathResults) {
            when {
                probe.isAccessible -> {
                    emitLog("[!] [HTTP ${probe.statusCode}] /${probe.path} ACCESSIBLE - ${probe.description}", LogSeverity.BAD)
                    vulnerabilities.add(
                        VulnerabilityFinding(
                            id = "PATH-EXPOSED-${probe.path.replace('/', '_').replace('.', '_')}",
                            titleEn = "Exposed Sensitive Endpoint: /${probe.path}",
                            titleAr = "ملف أو مسار حساس مكشوف للعامة: /${probe.path}",
                            owaspCategory = "A01: Broken Access Control",
                            severity = probe.riskLevel,
                            cvssScore = when (probe.riskLevel) {
                                SeverityLevel.CRITICAL -> 9.5
                                SeverityLevel.HIGH -> 7.8
                                SeverityLevel.MEDIUM -> 5.5
                                else -> 3.0
                            },
                            descriptionEn = "Endpoint /${probe.path} is publicly reachable (HTTP ${probe.statusCode}). ${probe.description}.",
                            descriptionAr = "المسار /${probe.path} متاح للجميع بدون تصريح (HTTP ${probe.statusCode}). ${probe.description}.",
                            remediationEn = "Block public access to sensitive files and directories in your web server configuration.",
                            remediationAr = "احظر الوصول العام لهذه الملفات الحساسة من خلال إعدادات الخادم.",
                            configSnippet = """
                                # Nginx Block Sensitive Files
                                location ~ /\.(env|git|htaccess) {
                                    deny all;
                                    return 404;
                                }
                            """.trimIndent()
                        )
                    )
                }
                probe.statusCode == 403 -> {
                    emitLog("[+] [HTTP 403] /${probe.path} Protected / Forbidden", LogSeverity.GOOD)
                }
                probe.statusCode != null -> {
                    emitLog("[+] [HTTP ${probe.statusCode}] /${probe.path}", LogSeverity.GOOD)
                }
                else -> {
                    emitLog("[-] [ERR] /${probe.path}", LogSeverity.INFO)
                }
            }
        }

        // Calculate CVSS-aligned score & statistics
        val criticalCount = vulnerabilities.count { it.severity == SeverityLevel.CRITICAL }
        val highCount = vulnerabilities.count { it.severity == SeverityLevel.HIGH }
        val mediumCount = vulnerabilities.count { it.severity == SeverityLevel.MEDIUM }
        val lowCount = vulnerabilities.count { it.severity == SeverityLevel.LOW }
        val warningCount = highCount + mediumCount

        var score = 100
        score -= (criticalCount * 25)
        score -= (highCount * 15)
        score -= (mediumCount * 7)
        score -= (lowCount * 3)
        val finalScore = score.coerceIn(5, 100)

        emitLog("\n=== SECURITY AUDIT COMPLETED ===", LogSeverity.HEADER, true)
        emitLog("Overall Security Score: $finalScore/100", if (finalScore >= 70) LogSeverity.GOOD else LogSeverity.BAD)
        emitLog("Findings: $criticalCount Critical | $highCount High | $mediumCount Medium | $lowCount Low", LogSeverity.INFO)

        val targetSummary = TargetSummary(
            requestedUrl = normalizedUrl,
            finalUrl = finalUrl,
            responseCode = statusCode,
            elapsedTimeSeconds = elapsedSec,
            isHttps = isHttps
        )

        val techCorsFinding = TechCorsFinding(
            serverBanner = serverBanner,
            xPoweredBy = xPoweredBy,
            corsHeader = corsHeader,
            isCorsWildcard = isCorsWildcard
        )

        val cookieFinding = CookieFinding(
            hasCookies = cookieHeaders.isNotEmpty(),
            cookieCount = cookieHeaders.size,
            httpOnlySet = httpOnlySet,
            secureSet = secureSet,
            sameSiteConfigured = sameSiteConfigured,
            details = cookieHeaders
        )

        AuditReport(
            target = targetSummary,
            securityHeaders = headerItems,
            techCors = techCorsFinding,
            cookies = cookieFinding,
            sensitivePaths = pathResults,
            sslInfo = sslInfo,
            rateLimit = rateLimitResult,
            vulnerabilities = vulnerabilities,
            rawLogs = rawLogs,
            score = finalScore,
            passedCount = passedHeaders + (if (isHttps) 1 else 0) + (if (serverBanner == null) 1 else 0),
            warningCount = warningCount,
            criticalCount = criticalCount
        )
    }

    private fun extractSslInfo(response: Response, isHttps: Boolean): SslCertificateInfo {
        if (!isHttps) {
            return SslCertificateInfo(isHttps = false)
        }

        return try {
            val handshake = response.handshake
            val protocol = handshake?.tlsVersion?.javaName ?: "TLS (Active)"
            val cipherSuite = handshake?.cipherSuite?.javaName ?: "Modern Cipher"
            val certs = handshake?.peerCertificates
            val primaryCert = certs?.firstOrNull() as? X509Certificate

            if (primaryCert != null) {
                val subject = primaryCert.subjectDN?.name ?: "Unknown Subject"
                val issuer = primaryCert.issuerDN?.name ?: "Unknown Issuer"
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val validFrom = sdf.format(primaryCert.notBefore)
                val validTo = sdf.format(primaryCert.notAfter)
                val now = System.currentTimeMillis()
                val daysRemaining = ((primaryCert.notAfter.time - now) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
                val isExpired = primaryCert.notAfter.before(Date())
                val isExpiringSoon = daysRemaining in 1..30
                val isSelfSigned = primaryCert.issuerDN == primaryCert.subjectDN

                // Clean issuer string for friendly display
                val cleanIssuer = when {
                    issuer.contains("Let's Encrypt", ignoreCase = true) -> "Let's Encrypt Authority"
                    issuer.contains("Cloudflare", ignoreCase = true) -> "Cloudflare Inc ECC CA"
                    issuer.contains("DigiCert", ignoreCase = true) -> "DigiCert Global CA"
                    issuer.contains("Google", ignoreCase = true) -> "Google Trust Services LLC"
                    issuer.contains("Sectigo", ignoreCase = true) -> "Sectigo RSA Domain Validation"
                    issuer.contains("cPanel", ignoreCase = true) -> "cPanel Inc Certification"
                    else -> issuer.split(",").firstOrNull { it.trim().startsWith("CN=") }?.replace("CN=", "") ?: issuer
                }

                SslCertificateInfo(
                    isHttps = true,
                    subject = subject.split(",").firstOrNull { it.trim().startsWith("CN=") }?.replace("CN=", "") ?: subject,
                    issuer = cleanIssuer,
                    validFrom = validFrom,
                    validTo = validTo,
                    daysRemaining = daysRemaining,
                    protocol = protocol,
                    cipherSuite = cipherSuite,
                    isExpired = isExpired,
                    isExpiringSoon = isExpiringSoon,
                    isSelfSigned = isSelfSigned
                )
            } else {
                SslCertificateInfo(
                    isHttps = true,
                    protocol = protocol,
                    cipherSuite = cipherSuite,
                    issuer = "Validated via HTTPS Handshake"
                )
            }
        } catch (e: Exception) {
            SslCertificateInfo(isHttps = true, protocol = "HTTPS Active")
        }
    }

    private fun evaluateRateLimiting(headers: okhttp3.Headers): RateLimitFinding {
        val limit = headers["X-RateLimit-Limit"] ?: headers["RateLimit-Limit"]
        val remaining = headers["X-RateLimit-Remaining"] ?: headers["RateLimit-Remaining"]
        val retryAfter = headers["Retry-After"]
        val cfRay = headers["cf-ray"]

        val hasHeaders = !limit.isNullOrBlank() || !remaining.isNullOrBlank() || !retryAfter.isNullOrBlank()
        val summaryEn = when {
            hasHeaders -> "Active Throttling (Limit: ${limit ?: "Custom"}, Remaining: ${remaining ?: "N/A"})"
            !cfRay.isNullOrBlank() -> "Cloudflare Edge Protection (WAF Active)"
            else -> "No Rate Limiting Headers detected"
        }
        val summaryAr = when {
            hasHeaders -> "نظام تقييد الطلبات نشط (الحد الأقصى: ${limit ?: "مخصص"})"
            !cfRay.isNullOrBlank() -> "حماية سحابية نشطة عبر Cloudflare WAF"
            else -> "لم يتم رصد رؤوس الحد من الطلبات"
        }

        return RateLimitFinding(
            isProtectionDetected = hasHeaders || !cfRay.isNullOrBlank(),
            limitHeader = limit,
            remainingHeader = remaining,
            retryAfterHeader = retryAfter,
            summaryAr = summaryAr,
            summaryEn = summaryEn
        )
    }

    private fun probeEndpoint(
        baseUrl: String,
        path: String,
        description: String,
        risk: SeverityLevel
    ): PathProbeResult {
        val targetUrl = "$baseUrl/$path"
        val request = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", USER_AGENT)
            .build()

        return try {
            noRedirectClient.newCall(request).execute().use { response ->
                val code = response.code
                val isAccessible = code in 200..299
                PathProbeResult(
                    path = path,
                    statusCode = code,
                    isAccessible = isAccessible,
                    riskLevel = risk,
                    description = description,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            PathProbeResult(
                path = path,
                statusCode = null,
                isAccessible = false,
                riskLevel = risk,
                description = description,
                errorMessage = e.message
            )
        }
    }
}
