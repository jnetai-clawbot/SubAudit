package com.jnetai.subaudit.ui.dashboard

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.jnetai.subaudit.R
import com.jnetai.subaudit.SubAuditApp
import com.jnetai.subaudit.data.Subscription
import com.jnetai.subaudit.ui.about.AboutActivity
import com.jnetai.subaudit.ui.addsubscription.AddSubscriptionActivity
import com.jnetai.subaudit.ui.notifications.ReminderReceiver
import com.jnetai.subaudit.ui.subscriptionlist.SubscriptionListActivity
import com.jnetai.subaudit.util.CurrencyUtils
import com.jnetai.subaudit.util.DateUtils
import com.jnetai.subaudit.util.DuplicateDetector
import com.jnetai.subaudit.util.ExportImportUtils
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class DashboardActivity : AppCompatActivity() {

    private lateinit var app: SubAuditApp
    private lateinit var tvTotalMonthly: TextView
    private lateinit var tvTotalYearly: TextView
    private lateinit var tvSubCount: TextView
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var rvUpcoming: RecyclerView
    private lateinit var tvNoUpcoming: TextView
    private lateinit var categoryContainer: LinearLayout
    private lateinit var btnAdd: com.google.android.material.floatingactionbutton.FloatingActionButton

    private var upcomingAdapter: UpcomingAdapter? = null
    private var allSubscriptions: List<Subscription> = emptyList()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        app = application as SubAuditApp

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        ReminderReceiver.createNotificationChannel(this)

        initViews()
        setupObservers()
        setupFilter()
    }

    private fun initViews() {
        tvTotalMonthly = findViewById(R.id.tvTotalMonthly)
        tvTotalYearly = findViewById(R.id.tvTotalYearly)
        tvSubCount = findViewById(R.id.tvSubCount)
        chipGroupFilter = findViewById(R.id.chipGroupFilter)
        rvUpcoming = findViewById(R.id.rvUpcoming)
        tvNoUpcoming = findViewById(R.id.tvNoUpcoming)
        categoryContainer = findViewById(R.id.categoryContainer)
        btnAdd = findViewById(R.id.fabAdd)

        rvUpcoming.layoutManager = LinearLayoutManager(this)
        upcomingAdapter = UpcomingAdapter { sub ->
            showSubscriptionOptions(sub)
        }
        rvUpcoming.adapter = upcomingAdapter

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddSubscriptionActivity::class.java))
        }
    }

    private fun setupObservers() {
        app.repository.allSubscriptions.observe(this) { subs ->
            allSubscriptions = subs
            updateDashboard(subs)
        }

        app.repository.getUpcomingPayments(
            DateUtils.startOfDay(),
            DateUtils.daysFromNow(7)
        ).observe(this) { upcoming ->
            if (upcoming.isEmpty()) {
                tvNoUpcoming.visibility = View.VISIBLE
                rvUpcoming.visibility = View.GONE
            } else {
                tvNoUpcoming.visibility = View.GONE
                rvUpcoming.visibility = View.VISIBLE
                upcomingAdapter?.submitList(upcoming)
            }
        }
    }

    private fun setupFilter() {
        val chips = listOf("All" to "all", "Next 7 days" to "7", "Next 30 days" to "30")
        chips.forEach { (label, tag) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                this.tag = tag
            }
            chipGroupFilter.addView(chip)
        }
        chipGroupFilter.setOnCheckedStateChangeListener { _, _ ->
            val checkedId = chipGroupFilter.checkedChipId
            val chip = chipGroupFilter.findViewById<Chip>(checkedId)
            filterSubscriptions(chip?.tag as? String ?: "all")
        }
        // Default: check "All"
        (chipGroupFilter.getChildAt(0) as Chip).isChecked = true
    }

    private fun filterSubscriptions(filter: String) {
        when (filter) {
            "7" -> {
                app.repository.getUpcomingPayments(
                    DateUtils.startOfDay(),
                    DateUtils.daysFromNow(7)
                ).observe(this) { upcoming ->
                    upcomingAdapter?.submitList(upcoming)
                    tvNoUpcoming.visibility = if (upcoming.isEmpty()) View.VISIBLE else View.GONE
                    rvUpcoming.visibility = if (upcoming.isEmpty()) View.GONE else View.VISIBLE
                }
            }
            "30" -> {
                app.repository.getUpcomingPayments(
                    DateUtils.startOfDay(),
                    DateUtils.daysFromNow(30)
                ).observe(this) { upcoming ->
                    upcomingAdapter?.submitList(upcoming)
                    tvNoUpcoming.visibility = if (upcoming.isEmpty()) View.VISIBLE else View.GONE
                    rvUpcoming.visibility = if (upcoming.isEmpty()) View.GONE else View.VISIBLE
                }
            }
            else -> {
                app.repository.activeSubscriptions.observe(this) { subs ->
                    upcomingAdapter?.submitList(subs)
                    tvNoUpcoming.visibility = if (subs.isEmpty()) View.VISIBLE else View.GONE
                    rvUpcoming.visibility = if (subs.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun updateDashboard(subs: List<Subscription>) {
        val activeSubs = subs.filter { it.status == "active" }
        val totalMonthly = activeSubs.sumOf { it.monthlyCost() }
        val totalYearly = activeSubs.sumOf { it.yearlyCost() }

        // Convert to primary display (use most common currency or GBP default)
        val monthlyByCurrency = activeSubs.groupBy { it.currency }.mapValues { entry ->
            entry.value.sumOf { it.monthlyCost() }
        }
        val yearlyByCurrency = activeSubs.groupBy { it.currency }.mapValues { entry ->
            entry.value.sumOf { it.yearlyCost() }
        }

        val monthlyStr = monthlyByCurrency.entries.joinToString(" + ") { entry ->
            CurrencyUtils.formatAmount(entry.value, entry.key)
        }
        val yearlyStr = yearlyByCurrency.entries.joinToString(" + ") { entry ->
            CurrencyUtils.formatAmount(entry.value, entry.key)
        }

        tvTotalMonthly.text = monthlyStr.ifEmpty { "£0.00" }
        tvTotalYearly.text = yearlyStr.ifEmpty { "£0.00" }
        tvSubCount.text = "${activeSubs.size} active subscription${if (activeSubs.size != 1) "s" else ""}"

        // Category breakdown
        categoryContainer.removeAllViews()
        val byCategory = activeSubs.groupBy { it.category }
        byCategory.entries.sortedByDescending { it.value.sumOf { s -> s.monthlyCost() } }.forEach { entry ->
            val catMonthly = entry.value.sumOf { it.monthlyCost() }
            val catCurrency = entry.value.firstOrNull()?.currency ?: "GBP"
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            val catName = TextView(this).apply {
                text = entry.key
                setTextColor(resources.getColor(R.color.md_on_surface, theme))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val catCost = TextView(this).apply {
                text = CurrencyUtils.formatAmount(catMonthly, catCurrency) + "/mo"
                setTextColor(resources.getColor(R.color.md_primary, theme))
                textSize = 14f
                gravity = android.view.Gravity.END
            }
            row.addView(catName)
            row.addView(catCost)
            categoryContainer.addView(row)
        }
    }

    private fun showSubscriptionOptions(sub: Subscription) {
        val options = arrayOf("Active", "Paused", "Cancelled", "Delete")
        val currentIdx = when (sub.status) {
            "active" -> 0
            "paused" -> 1
            "cancelled" -> 2
            else -> 0
        }
        AlertDialog.Builder(this)
            .setTitle(sub.name)
            .setSingleChoiceItems(options, currentIdx) { dialog, which ->
                lifecycleScope.launch {
                    when (which) {
                        0 -> app.repository.update(sub.copy(status = "active"))
                        1 -> app.repository.update(sub.copy(status = "paused"))
                        2 -> app.repository.update(sub.copy(status = "cancelled"))
                    }
                    // Recalculate next payment for active subs
                    if (which == 0) {
                        ReminderReceiver.scheduleReminder(
                            this@DashboardActivity, sub.id, sub.name, sub.cost, sub.currency, sub.nextPaymentDate
                        )
                    } else {
                        ReminderReceiver.cancelReminder(this@DashboardActivity, sub.id)
                    }
                }
                dialog.dismiss()
            }
            .setNeutralButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    app.repository.delete(sub)
                    ReminderReceiver.cancelReminder(this@DashboardActivity, sub.id)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_all_subs -> {
                startActivity(Intent(this, SubscriptionListActivity::class.java))
                true
            }
            R.id.action_export -> {
                exportData()
                true
            }
            R.id.action_import -> {
                importData()
                true
            }
            R.id.action_duplicates -> {
                checkDuplicates()
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportData() {
        lifecycleScope.launch {
            val subs = app.repository.getAllSync()
            val json = ExportImportUtils.exportToJson(subs)
            try {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "subaudit_export.json")
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                exportLauncher.launch(intent)
                pendingExportData = json
            } catch (e: Exception) {
                // Fallback to share
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                startActivity(Intent.createChooser(shareIntent, "Export Subscriptions"))
            }
        }
    }

    private var pendingExportData: String? = null

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                pendingExportData?.let { json ->
                    contentResolver.openOutputStream(uri)?.use { os ->
                        OutputStreamWriter(os).use { it.write(json) }
                    }
                    Toast.makeText(this, "Export successful", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun importData() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        importLauncher.launch(intent)
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                lifecycleScope.launch {
                    try {
                        val json = contentResolver.openInputStream(uri)?.use { is_ ->
                            BufferedReader(InputStreamReader(is_)).readText()
                        } ?: return@launch
                        val subs = ExportImportUtils.importFromJson(json)
                        if (subs != null) {
                            app.repository.importAll(subs)
                            // Schedule reminders
                            subs.filter { it.status == "active" }.forEach { sub ->
                                ReminderReceiver.scheduleReminder(
                                    this@DashboardActivity, sub.id, sub.name, sub.cost, sub.currency, sub.nextPaymentDate
                                )
                            }
                            Toast.makeText(this@DashboardActivity, "Imported ${subs.size} subscriptions", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@DashboardActivity, "Invalid import file", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@DashboardActivity, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun checkDuplicates() {
        lifecycleScope.launch {
            val subs = app.repository.getAllSync()
            val duplicates = DuplicateDetector.findDuplicates(subs)
            if (duplicates.isEmpty()) {
                AlertDialog.Builder(this@DashboardActivity)
                    .setTitle("Duplicate Check")
                    .setMessage("No duplicate subscriptions found! 🎉")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                val msg = duplicates.joinToString("\n\n") { group ->
                    val names = (listOf(group.original) + group.duplicates).joinToString(", ") { it.name }
                    "⚠️ Similar: $names"
                }
                AlertDialog.Builder(this@DashboardActivity)
                    .setTitle("Potential Duplicates")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}