package com.jnetai.subaudit.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import java.util.UUID

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val cost: Double,
    val billingCycle: String, // "weekly", "monthly", "yearly"
    val category: String,
    val nextPaymentDate: Long, // epoch millis
    val status: String = "active", // "active", "paused", "cancelled"
    val currency: String = "GBP", // "GBP", "USD", "EUR"
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: UUID.randomUUID().toString(),
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readString() ?: "monthly",
        parcel.readString() ?: "Other",
        parcel.readLong(),
        parcel.readString() ?: "active",
        parcel.readString() ?: "GBP",
        parcel.readLong(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeDouble(cost)
        parcel.writeString(billingCycle)
        parcel.writeString(category)
        parcel.writeLong(nextPaymentDate)
        parcel.writeString(status)
        parcel.writeString(currency)
        parcel.writeLong(createdAt)
        parcel.writeString(notes)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Subscription> {
        override fun createFromParcel(parcel: Parcel): Subscription = Subscription(parcel)
        override fun newArray(size: Int): Array<Subscription?> = arrayOfNulls(size)
    }

    fun monthlyCost(): Double {
        return when (billingCycle.lowercase()) {
            "weekly" -> cost * 52.0 / 12.0
            "monthly" -> cost
            "yearly" -> cost / 12.0
            else -> 0.0
        }
    }

    fun yearlyCost(): Double {
        return when (billingCycle.lowercase()) {
            "weekly" -> cost * 52.0
            "monthly" -> cost * 12.0
            "yearly" -> cost
            else -> 0.0
        }
    }
}