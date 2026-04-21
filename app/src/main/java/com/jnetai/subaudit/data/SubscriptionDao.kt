package com.jnetai.subaudit.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextPaymentDate ASC")
    fun getAllSubscriptions(): LiveData<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE status = 'active' ORDER BY nextPaymentDate ASC")
    fun getActiveSubscriptions(): LiveData<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE status = :status ORDER BY nextPaymentDate ASC")
    fun getByStatus(status: String): LiveData<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE nextPaymentDate BETWEEN :from AND :to AND status = 'active' ORDER BY nextPaymentDate ASC")
    fun getUpcomingPayments(from: Long, to: Long): LiveData<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: String): Subscription?

    @Query("SELECT DISTINCT category FROM subscriptions ORDER BY category ASC")
    fun getCategories(): LiveData<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: Subscription)

    @Update
    suspend fun update(subscription: Subscription)

    @Delete
    suspend fun delete(subscription: Subscription)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM subscriptions")
    suspend fun getAllSync(): List<Subscription>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subscriptions: List<Subscription>)

    @Query("SELECT * FROM subscriptions WHERE LOWER(name) LIKE LOWER(:query) OR LOWER(name) LIKE LOWER(:query2)")
    suspend fun searchByName(query: String, query2: String): List<Subscription>
}