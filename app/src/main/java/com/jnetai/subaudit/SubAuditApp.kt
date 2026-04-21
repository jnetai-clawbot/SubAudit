package com.jnetai.subaudit

import android.app.Application
import com.jnetai.subaudit.data.AppDatabase
import com.jnetai.subaudit.data.SubscriptionRepository

class SubAuditApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { SubscriptionRepository(database.subscriptionDao()) }
}