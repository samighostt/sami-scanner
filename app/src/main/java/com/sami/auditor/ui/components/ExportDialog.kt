package com.sami.auditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sami.auditor.ui.ExportFormat
import com.sami.auditor.ui.theme.SamiBackground
import com.sami.auditor.ui.theme.SamiCardBg
import com.sami.auditor.ui.theme.SamiCardBorder
import com.sami.auditor.ui.theme.SamiCardElevated
import com.sami.auditor.ui.theme.SamiGold
import com.sami.auditor.ui.theme.SamiMuted
import com.sami.auditor.ui.theme.SamiText

@Composable
fun ExportReportDialog(
    onDismiss: () -> Unit,
    onSelectFormat: (ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SamiCardElevated,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = SamiGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تصدير ومشاركة التقرير الأمني",
                        color = SamiGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SamiMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اختر الصيغة المناسبة لحفظ أو مشاركة تقرير الفحص الأمني لتقديمه للعميل أو الاحتفاظ به:",
                    color = SamiMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                ExportFormatOption(
                    title = "تقرير HTML تفاعلي (Interactive Web Report)",
                    subtitle = "تصميم احترافي جاهز للفتح في المتصفح أو الطباعة كملف PDF",
                    icon = Icons.Default.Language,
                    color = SamiGold,
                    onClick = { onSelectFormat(ExportFormat.HTML) }
                )

                ExportFormatOption(
                    title = "تصدير بيانات JSON (Raw JSON Data)",
                    subtitle = "تنسيق مهيكل ومتكامل للربط البرمجي والأرشفة الرقمية",
                    icon = Icons.Default.Code,
                    color = Color(0xFF38BDF8),
                    onClick = { onSelectFormat(ExportFormat.JSON) }
                )

                ExportFormatOption(
                    title = "ملخص نصي للمراسلة (Summary Text)",
                    subtitle = "ملخص سريع لنتائج الفحص للمشاركة عبر واتساب أو البريد",
                    icon = Icons.Default.Description,
                    color = Color(0xFF10B981),
                    onClick = { onSelectFormat(ExportFormat.TEXT) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SamiCardBg,
                    contentColor = SamiText
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("إلغاء", fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun ExportFormatOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SamiCardBg)
            .border(1.dp, SamiCardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SamiText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = SamiMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
