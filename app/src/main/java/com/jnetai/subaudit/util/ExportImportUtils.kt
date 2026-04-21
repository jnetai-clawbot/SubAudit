package com.jnetai.subaudit.util

import com.jnetai.subaudit.data.Subscription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ExportImportUtils {
    private val gson = Gson()

    data class ExportData(
        val version: Int = 1,
        val exportDate: Long = System.currentTimeMillis(),
        val subscriptions: List<Subscription>
    )

    fun exportToJson(subscriptions: List<Subscription>): String {
        val data = ExportData(subscriptions = subscriptions)
        return gson.toJson(data)
    }

    fun importFromJson(json: String): List<Subscription>? {
        return try {
            val type = object : TypeToken<ExportData>() {}.type
            val data: ExportData = gson.fromJson(json, type)
            data.subscriptions
        } catch (e: Exception) {
            null
        }
    }
}