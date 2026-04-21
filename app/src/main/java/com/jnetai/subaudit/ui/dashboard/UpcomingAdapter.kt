package com.jnetai.subaudit.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.subaudit.R
import com.jnetai.subaudit.data.Subscription
import com.jnetai.subaudit.util.CurrencyUtils
import com.jnetai.subaudit.util.DateUtils

class UpcomingAdapter(private val onClick: (Subscription) -> Unit) :
    RecyclerView.Adapter<UpcomingAdapter.ViewHolder>() {

    private var items: List<Subscription> = emptyList()

    fun submitList(newItems: List<Subscription>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvSubName)
        private val tvCost: TextView = view.findViewById(R.id.tvSubCost)
        private val tvDate: TextView = view.findViewById(R.id.tvSubDate)
        private val tvCategory: TextView = view.findViewById(R.id.tvSubCategory)
        private val tvStatus: TextView = view.findViewById(R.id.tvSubStatus)
        private val tvCycle: TextView = view.findViewById(R.id.tvSubCycle)

        fun bind(sub: Subscription) {
            tvName.text = sub.name
            tvCost.text = CurrencyUtils.formatAmount(sub.cost, sub.currency)
            tvDate.text = DateUtils.formatDate(sub.nextPaymentDate)
            tvCategory.text = sub.category
            tvCycle.text = sub.billingCycle.capitalize()

            val days = DateUtils.daysUntil(sub.nextPaymentDate)
            tvStatus.text = when {
                sub.status == "paused" -> "⏸ Paused"
                sub.status == "cancelled" -> "✗ Cancelled"
                days < 0 -> "⚠️ Overdue"
                days == 0 -> "🔴 Today"
                days <= 3 -> "🟡 In $days day${if (days != 1) "s" else ""}"
                else -> "🟢 In $days days"
            }

            itemView.setOnClickListener { onClick(sub) }
        }
    }
}