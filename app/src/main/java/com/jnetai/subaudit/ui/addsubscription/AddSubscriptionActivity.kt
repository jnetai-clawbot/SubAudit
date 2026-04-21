package com.jnetai.subaudit.ui.addsubscription

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.jnetai.subaudit.R
import com.jnetai.subaudit.SubAuditApp
import com.jnetai.subaudit.data.Subscription
import com.jnetai.subaudit.ui.notifications.ReminderReceiver
import com.jnetai.subaudit.util.CurrencyUtils
import kotlinx.coroutines.launch
import java.util.*

class AddSubscriptionActivity : AppCompatActivity() {

    private lateinit var app: SubAuditApp
    private lateinit var etName: TextInputEditText
    private lateinit var etCost: TextInputEditText
    private lateinit var spinnerCycle: MaterialAutoCompleteTextView
    private lateinit var spinnerCurrency: MaterialAutoCompleteTextView
    private lateinit var etCategory: MaterialAutoCompleteTextView
    private lateinit var etNotes: TextInputEditText
    private lateinit var tvDate: TextView
    private lateinit var btnSave: Button

    private var selectedDate: Long = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L
    private var editSubscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_subscription)

        app = application as SubAuditApp
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupSpinners()
        setupDatePicker()

        // Check if editing
        editSubscription = intent.getParcelableExtra("subscription")
        editSubscription?.let { populateForEdit(it) }

        btnSave.setOnClickListener { saveSubscription() }
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etCost = findViewById(R.id.etCost)
        spinnerCycle = findViewById(R.id.spinnerCycle)
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        etCategory = findViewById(R.id.etCategory)
        etNotes = findViewById(R.id.etNotes)
        tvDate = findViewById(R.id.tvDate)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupSpinners() {
        val cycles = listOf("weekly", "monthly", "yearly")
        val cycleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cycles)
        spinnerCycle.setAdapter(cycleAdapter)
        spinnerCycle.setText("monthly", false)

        val currencies = CurrencyUtils.CURRENCIES.map { "${it.code} (${it.symbol}) - ${it.name}" }
        val currencyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        spinnerCurrency.setAdapter(currencyAdapter)
        spinnerCurrency.setText(currencies[0], false)

        val categories = listOf(
            "Streaming", "Music", "Cloud Storage", "Software", "Gaming",
            "News", "Fitness", "Productivity", "VPN", "Hosting",
            "Domain", "Email", "Other"
        )
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        etCategory.setAdapter(catAdapter)
    }

    private fun setupDatePicker() {
        updateDateDisplay()
        val dateLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilDate)
        tvDate.setOnClickListener { showDatePicker() }
        dateLayout?.setEndIconOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val c = Calendar.getInstance().apply {
                    set(year, month, day, 12, 0, 0)
                }
                selectedDate = c.timeInMillis
                updateDateDisplay()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        tvDate.text = String.format(
            "%02d/%02d/%04d",
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
    }

    private fun populateForEdit(sub: Subscription) {
        etName.setText(sub.name)
        etCost.setText(sub.cost.toString())
        spinnerCycle.setText(sub.billingCycle, false)
        etCategory.setText(sub.category, false)
        etNotes.setText(sub.notes)
        selectedDate = sub.nextPaymentDate
        updateDateDisplay()

        val currStr = CurrencyUtils.CURRENCIES.find { it.code == sub.currency }
            ?.let { "${it.code} (${it.symbol}) - ${it.name}" } ?: CurrencyUtils.CURRENCIES[0].let { "${it.code} (${it.symbol}) - ${it.name}" }
        spinnerCurrency.setText(currStr, false)

        btnSave.text = "Update"
        title = "Edit Subscription"
    }

    private fun saveSubscription() {
        val name = etName.text?.toString()?.trim()
        if (name.isNullOrBlank()) {
            etName.error = "Name is required"
            return
        }
        val costStr = etCost.text?.toString()?.trim()
        if (costStr.isNullOrBlank()) {
            etCost.error = "Cost is required"
            return
        }
        val cost = costStr.toDoubleOrNull()
        if (cost == null || cost < 0) {
            etCost.error = "Invalid cost"
            return
        }

        val cycle = spinnerCycle.text?.toString()?.lowercase()?.trim() ?: "monthly"
        val currencyFull = spinnerCurrency.text?.toString() ?: ""
        val currency = currencyFull.substringBefore(" ").ifBlank { "GBP" }
        val category = etCategory.text?.toString()?.trim() ?: "Other"
        val notes = etNotes.text?.toString()?.trim() ?: ""

        lifecycleScope.launch {
            val sub = editSubscription?.copy(
                name = name,
                cost = cost,
                billingCycle = cycle,
                category = category,
                nextPaymentDate = selectedDate,
                currency = currency,
                notes = notes
            ) ?: Subscription(
                name = name,
                cost = cost,
                billingCycle = cycle,
                category = category,
                nextPaymentDate = selectedDate,
                currency = currency,
                notes = notes
            )

            if (editSubscription != null) {
                app.repository.update(sub)
            } else {
                app.repository.insert(sub)
            }

            if (sub.status == "active") {
                ReminderReceiver.scheduleReminder(
                    this@AddSubscriptionActivity,
                    sub.id, sub.name, sub.cost, sub.currency, sub.nextPaymentDate
                )
            }

            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}