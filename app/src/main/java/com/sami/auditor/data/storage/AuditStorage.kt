package com.sami.auditor.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.sami.auditor.data.model.AuditHistoryItem
import com.sami.auditor.data.model.AuditReport
import com.sami.auditor.data.model.MonitoredSite
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AuditStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sami_auditor_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "audit_history_json"
        private const val KEY_MONITORED = "monitored_sites_json"
    }

    // Save scan to history
    fun saveScanHistory(report: AuditReport) {
        val history = getScanHistory().toMutableList()
        // Avoid duplicate ID
        history.removeAll { it.id == report.id }
        history.add(
            0,
            AuditHistoryItem(
                id = report.id,
                url = report.target.finalUrl.ifBlank { report.target.requestedUrl },
                timestamp = report.timestamp,
                score = report.score,
                criticalCount = report.criticalCount,
                warningCount = report.warningCount,
                passedCount = report.passedCount
            )
        )
        // Keep last 30 scans
        val trimmed = history.take(30)
        val array = JSONArray()
        trimmed.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("url", item.url)
            obj.put("timestamp", item.timestamp)
            obj.put("score", item.score)
            obj.put("criticalCount", item.criticalCount)
            obj.put("warningCount", item.warningCount)
            obj.put("passedCount", item.passedCount)
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun getScanHistory(): List<AuditHistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AuditHistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AuditHistoryItem(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        url = obj.getString("url"),
                        timestamp = obj.getLong("timestamp"),
                        score = obj.getInt("score"),
                        criticalCount = obj.getInt("criticalCount"),
                        warningCount = obj.getInt("warningCount"),
                        passedCount = obj.getInt("passedCount")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    // Monitored sites
    fun getMonitoredSites(): List<MonitoredSite> {
        val raw = prefs.getString(KEY_MONITORED, null)
        if (raw == null) {
            // Default sample monitored sites
            val defaultList = listOf(
                MonitoredSite(
                    url = "https://example.com",
                    intervalHours = 24,
                    lastChecked = System.currentTimeMillis() - 3600_000 * 5,
                    lastScore = 85,
                    status = "SECURE"
                )
            )
            saveMonitoredSites(defaultList)
            return defaultList
        }
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<MonitoredSite>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MonitoredSite(
                        id = obj.getString("id"),
                        url = obj.getString("url"),
                        intervalHours = obj.optInt("intervalHours", 24),
                        lastChecked = obj.optLong("lastChecked", 0L),
                        lastScore = obj.optInt("lastScore", 0),
                        status = obj.optString("status", "PENDING"),
                        isActive = obj.optBoolean("isActive", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMonitoredSites(sites: List<MonitoredSite>) {
        val array = JSONArray()
        sites.forEach { site ->
            val obj = JSONObject()
            obj.put("id", site.id)
            obj.put("url", site.url)
            obj.put("intervalHours", site.intervalHours)
            obj.put("lastChecked", site.lastChecked)
            obj.put("lastScore", site.lastScore)
            obj.put("status", site.status)
            obj.put("isActive", site.isActive)
            array.put(obj)
        }
        prefs.edit().putString(KEY_MONITORED, array.toString()).apply()
    }

    fun addMonitoredSite(url: String, intervalHours: Int): List<MonitoredSite> {
        val current = getMonitoredSites().toMutableList()
        current.removeAll { it.url.equals(url, ignoreCase = true) }
        current.add(
            0,
            MonitoredSite(
                url = url,
                intervalHours = intervalHours,
                lastChecked = 0L,
                status = "PENDING"
            )
        )
        saveMonitoredSites(current)
        return current
    }

    fun removeMonitoredSite(id: String): List<MonitoredSite> {
        val current = getMonitoredSites().toMutableList()
        current.removeAll { it.id == id }
        saveMonitoredSites(current)
        return current
    }

    // Save report file to app storage and return the File
    fun writeReportFile(context: Context, filename: String, content: String): File {
        val dir = File(context.filesDir, "reports").apply { mkdirs() }
        val file = File(dir, filename)
        file.writeText(content, Charsets.UTF_8)
        return file
    }
}
