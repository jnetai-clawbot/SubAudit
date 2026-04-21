package com.jnetai.subaudit.ui.about

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jnetai.subaudit.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class AboutActivity : AppCompatActivity() {

    private lateinit var tvVersion: TextView
    private lateinit var btnCheckUpdate: Button
    private lateinit var btnShare: Button
    private lateinit var tvUpdateStatus: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "About SubAudit"

        tvVersion = findViewById(R.id.tvVersion)
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate)
        btnShare = findViewById(R.id.btnShare)
        tvUpdateStatus = findViewById(R.id.tvUpdateStatus)
        progressBar = findViewById(R.id.progressBar)

        // Get version from PackageInfo
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = "Version ${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            tvVersion.text = "Version unknown"
        }

        btnCheckUpdate.setOnClickListener { checkForUpdates() }
        btnShare.setOnClickListener { shareApp() }
    }

    private fun checkForUpdates() {
        progressBar.visibility = View.VISIBLE
        tvUpdateStatus.text = "Checking..."
        btnCheckUpdate.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://api.github.com/repos/jnetai-clawbot/SubAudit/releases/latest")
                    val connection = url.openConnection()
                    connection.setRequestProperty("Accept", "application/vnd.github+json")
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    val response = connection.getInputStream().bufferedReader().readText()
                    val json = JSONObject(response)
                    val tagName = json.optString("tag_name", "unknown")
                    val htmlUrl = json.optString("html_url", "")
                    val body = json.optString("body", "")
                    Pair(tagName, htmlUrl)
                }

                val (remoteVersion, releaseUrl) = result
                val currentVersion = try {
                    packageManager.getPackageInfo(packageName, 0).versionName
                } catch (_: Exception) { "0.0.0" }

                if (remoteVersion != "unknown" && remoteVersion != currentVersion) {
                    tvUpdateStatus.text = "Update available: $remoteVersion\nTap to download"
                    tvUpdateStatus.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(releaseUrl))
                        startActivity(intent)
                    }
                } else {
                    tvUpdateStatus.text = "You're up to date ✓"
                    tvUpdateStatus.setOnClickListener(null)
                }
            } catch (e: Exception) {
                tvUpdateStatus.text = "Check failed: ${e.message}"
            } finally {
                progressBar.visibility = View.GONE
                btnCheckUpdate.isEnabled = true
            }
        }
    }

    private fun shareApp() {
        val shareText = """
            Check out SubAudit - Subscription Tracker!
            Track all your subscriptions in one place.
            https://github.com/jnetai-clawbot/SubAudit
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share SubAudit"))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}