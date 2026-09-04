package com.sami.auditor.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sami.auditor.data.model.AuditReport
import com.sami.auditor.data.model.PathProbeResult
import com.sami.auditor.data.model.RateLimitFinding
import com.sami.auditor.data.model.SecurityHeaderItem
import com.sami.auditor.data.model.SeverityLevel
import com.sami.auditor.data.model.SslCertificateInfo
import com.sami.auditor.ui.theme.SamiBad
import com.sami.auditor.ui.theme.SamiCardBg
import com.sami.auditor.ui.theme.SamiCardBorder
import com.sami.auditor.ui.theme.SamiGold
import com.sami.auditor.ui.theme.SamiGood
import com.sami.auditor.ui.theme.SamiInfo
import com.sami.auditor.ui.theme.SamiMuted
import com.sami.auditor.ui.theme.SamiText
import com.sami.auditor.ui.theme.SamiWarn

@Composable
fun SslAndPathsView(report: AuditReport?) {
    if (report == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ابدأ الفحص لعرض تفاصيل التشفير SSL والمسارات الحساسة",
                color = SamiMuted,
                fontSize = 13.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SSL Certificate Card
        item {
            SslCertificateSection(ssl = report.sslInfo)
        }

        // Rate Limiting Defense Card
        item {
            RateLimitingSection(rateLimit = report.rateLimit)
        }

        // Sensitive Paths Table
        item {
            SensitivePathsSection(paths = report.sensitivePaths)
        }

        // Security Headers Table
        item {
            SecurityHeadersSection(headers = report.securityHeaders)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SslCertificateSection(ssl: SslCertificateInfo) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SamiCardBg)
            .border(1.dp, SamiCardBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (ssl.isHttps) Icons.Default.Https else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (ssl.isHttps) SamiGood else SamiBad,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شهادة التشفير والأمان SSL/TLS",
                        color = SamiGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (ssl.isHttps) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (ssl.daysRemaining < 30) SamiWarn else SamiGood)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${ssl.daysRemaining} يوم متبقي",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!ssl.isHttps) {
                Text(
                    text = "الاتصال غير مشفر (HTTP عادي). لا توجد شهادة SSL مطبقة، مما يعرض الموقع للمخاطر الأمنية.",
                    color = SamiBad,
                    fontSize = 12.sp
                )
            } else {
                InfoKeyValueRow("الجهة المصدرة (Issuer):", ssl.issuer)
                InfoKeyValueRow("النطاق (Subject):", ssl.subject)
                InfoKeyValueRow("البروتوكول (Protocol):", ssl.protocol)
                InfoKeyValueRow("خوارزمية التشفير (Cipher):", ssl.cipherSuite)
                InfoKeyValueRow("تاريخ الانتهاء:", ssl.validTo)
                if (ssl.isSelfSigned) {
                    InfoKeyValueRow("شهادة موقعة ذاتياً:", "نعم (تحذير: غير معتمدة من جهة موثوقة)", isWarning = true)
                }
            }
        }
    }
}

@Composable
private fun RateLimitingSection(rateLimit: RateLimitFinding) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SamiCardBg)
            .border(1.dp, SamiCardBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = if (rateLimit.isProtectionDetected) SamiGood else SamiWarn,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "مقاومة هجمات التخمين وتحديد الطلبات (Rate Limiting)",
                    color = SamiGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = rateLimit.summaryAr,
                color = if (rateLimit.isProtectionDetected) SamiGood else SamiWarn,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (!rateLimit.limitHeader.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                InfoKeyValueRow("الحد الأقصى المسموح (Limit):", rateLimit.limitHeader)
            }
            if (!rateLimit.remainingHeader.isNullOrBlank()) {
                InfoKeyValueRow("الطلبات المتبقية (Remaining):", rateLimit.remainingHeader)
            }
            if (!rateLimit.retryAfterHeader.isNullOrBlank()) {
                InfoKeyValueRow("إعادة المحاولة بعد (Retry-After):", "${rateLimit.retryAfterHeader}s")
            }
        }
    }
}

@Composable
private fun SensitivePathsSection(paths: List<PathProbeResult>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SamiCardBg)
            .border(1.dp, SamiCardBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = SamiGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "اكتشاف الملفات والمسارات الحساسة (Recon & Discovery)",
                    color = SamiGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            paths.forEach { probe ->
                val badgeColor = when {
                    probe.isAccessible -> SamiBad
                    probe.statusCode == 403 -> SamiWarn
                    probe.statusCode == 404 -> SamiGood
                    else -> SamiMuted
                }
                val statusText = when {
                    probe.isAccessible -> "مكشوف [HTTP ${probe.statusCode}]"
                    probe.statusCode != null -> "محمي [HTTP ${probe.statusCode}]"
                    else -> "غير متاح"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "/${probe.path}",
                            color = SamiText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = probe.description,
                            color = SamiMuted,
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityHeadersSection(headers: List<SecurityHeaderItem>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SamiCardBg)
            .border(1.dp, SamiCardBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SamiGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تحليل رؤوس الأمان (Security Headers)",
                    color = SamiGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            headers.forEach { header ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = header.name,
                            color = SamiText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = header.explanationAr,
                            color = SamiMuted,
                            fontSize = 10.sp
                        )
                    }

                    Icon(
                        imageVector = if (header.isPresent) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (header.isPresent) SamiGood else SamiBad,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoKeyValueRow(key: String, value: String, isWarning: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = key, color = SamiMuted, fontSize = 11.sp)
        Text(
            text = value,
            color = if (isWarning) SamiWarn else SamiText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
