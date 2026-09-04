package com.sami.auditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sami.auditor.ui.AuditScreen
import com.sami.auditor.ui.AuditViewModel
import com.sami.auditor.ui.theme.SAMIAuditorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SAMIAuditorTheme {
                val viewModel: AuditViewModel = viewModel()
                AuditScreen(viewModel = viewModel)
            }
        }
    }
}
