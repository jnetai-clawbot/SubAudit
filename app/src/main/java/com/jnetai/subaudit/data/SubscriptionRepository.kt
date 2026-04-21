package com.jnetai.subaudit.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubscriptionRepository(private val dao: SubscriptionDao) {

    val allSubscriptions: LiveData<List<Subscription>> = dao.getAllSubscriptions()
    val activeSubscriptions: LiveData<List<Subscription>> = dao.getActiveSubscriptions()
    val categories: LiveData<List<String>> = dao.getCategories()

    fun getByStatus(status: String): LiveData<List<Subscription>> = dao.getByStatus(status)
    fun getUpcomingPayments(from: Long, to: Long): LiveData<List<Subscription>> = dao.getUpcomingPayments(from, to)

    suspend fun insert(subscription: Subscription) = withContext(Dispatchers.IO) {
        dao.insert(subscription)
    }

    suspend fun update(subscription: Subscription) = withContext(Dispatchers.IO) {
        dao.update(subscription)
    }

    suspend fun delete(subscription: Subscription) = withContext(Dispatchers.IO) {
        dao.delete(subscription)
    }

    suspend fun deleteById(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun getAllSync(): List<Subscription> = withContext(Dispatchers.IO) {
        dao.getAllSync()
    }

    suspend fun importAll(subscriptions: List<Subscription>) = withContext(Dispatchers.IO) {
        dao.insertAll(subscriptions)
    }

    suspend fun findSimilar(name: String): List<Subscription> = withContext(Dispatchers.IO) {
        dao.searchByName("%$name%", "%${name.take(4)}%")
    }
}