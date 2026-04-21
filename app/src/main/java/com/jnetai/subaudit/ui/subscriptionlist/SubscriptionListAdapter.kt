package com.jnetai.subaudit.ui.subscriptionlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.subaudit.R
import com.jnetai.subaudit.data.Subscription
import com.jnetai.subaudit.util.CurrencyUtils
import com.jnetai.subaudit.util.DateUtils

class SubscriptionListAdapter(
    private val onClick: (Subscription) -> Unit,
    private val onStatusChange: (Subscription, String) -> Unit,
    private val onDelete: (Subscription) -> Unit
) : RecyclerView.Adapter<SubscriptionListAdapter.ViewHolder>() {

    private var items: List<Subscription> = emptyList()

    fun submitList(newItems: List<Subscription>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription_list, parent, false)
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
        private val btnPause: ImageButton = view.findViewById(R.id.btnPause)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        fun bind(sub: Subscription) {
            tvName.text = sub.name
            tvCost.text = CurrencyUtils.formatAmount(sub.cost, sub.currency)
            tvDate.text = DateUtils.formatDate(sub.nextPaymentDate)
            tvCategory.text = sub.category
            tvCycle.text = sub.billingCycle.capitalize()

            val statusIcon = when (sub.status) {
                "paused" -> "⏸"
                "cancelled" -> "✗"
                else -> "●"
            }
            tvStatus.text = "$statusIcon ${sub.status.capitalize()}"

            itemView.setOnClickListener { onClick(sub) }

            btnPause.setOnClickListener {
                val newStatus = when (sub.status) {
                    "active" -> "paused"
                    "paused" -> "active"
                    else -> "active"
                }
                onStatusChange(sub, newStatus)
            }

            btnPause.contentDescription = when (sub.status) {
                "active" -> "Pause"
                "paused" -> "Resume"
                else -> "Activate"
            }

            btnDelete.setOnClickListener { onDelete(sub) }
        }
    }
}