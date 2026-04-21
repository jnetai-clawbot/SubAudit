package com.jnetai.subaudit.ui.subscriptionlist

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.subaudit.R
import com.jnetai.subaudit.SubAuditApp
import com.jnetai.subaudit.data.Subscription
import com.jnetai.subaudit.ui.addsubscription.AddSubscriptionActivity
import com.jnetai.subaudit.ui.notifications.ReminderReceiver
import kotlinx.coroutines.launch

class SubscriptionListActivity : AppCompatActivity() {

    private lateinit var app: SubAuditApp
    private lateinit var rvSubs: RecyclerView
    private lateinit var spinnerFilter: Spinner
    private lateinit var adapter: SubscriptionListAdapter
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription_list)

        app = application as SubAuditApp
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "All Subscriptions"

        rvSubs = findViewById(R.id.rvSubs)
        spinnerFilter = findViewById(R.id.spinnerFilter)

        rvSubs.layoutManager = LinearLayoutManager(this)
        adapter = SubscriptionListAdapter(
            onClick = { sub -> openEdit(sub) },
            onStatusChange = { sub, status -> changeStatus(sub, status) },
            onDelete = { sub -> deleteSub(sub) }
        )
        rvSubs.adapter = adapter

        setupFilter()
        observeData()
    }

    private fun setupFilter() {
        val filters = arrayOf("All", "Active", "Paused", "Cancelled")
        spinnerFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filters)
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                currentFilter = filters[pos].lowercase()
                observeData()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeData() {
        when (currentFilter) {
            "active" -> app.repository.getByStatus("active")
            "paused" -> app.repository.getByStatus("paused")
            "cancelled" -> app.repository.getByStatus("cancelled")
            else -> app.repository.allSubscriptions
        }.observe(this) { subs ->
            adapter.submitList(subs)
        }
    }

    private fun openEdit(sub: Subscription) {
        val intent = Intent(this, AddSubscriptionActivity::class.java)
        intent.putExtra("subscription", sub)
        startActivity(intent)
    }

    private fun changeStatus(sub: Subscription, status: String) {
        lifecycleScope.launch {
            app.repository.update(sub.copy(status = status))
            if (status == "active") {
                ReminderReceiver.scheduleReminder(
                    this@SubscriptionListActivity, sub.id, sub.name, sub.cost, sub.currency, sub.nextPaymentDate
                )
            } else {
                ReminderReceiver.cancelReminder(this@SubscriptionListActivity, sub.id)
            }
        }
    }

    private fun deleteSub(sub: Subscription) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${sub.name}?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    app.repository.delete(sub)
                    ReminderReceiver.cancelReminder(this@SubscriptionListActivity, sub.id)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}