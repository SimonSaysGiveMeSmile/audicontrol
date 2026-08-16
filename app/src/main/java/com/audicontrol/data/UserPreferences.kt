package com.audicontrol.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("audicontrol_prefs", Context.MODE_PRIVATE)

    var savedVin: String?
        get() = prefs.getString("saved_vin", null)
        set(value) = prefs.edit().putString("saved_vin", value).apply()

    var savedMake: String?
        get() = prefs.getString("saved_make", null)
        set(value) = prefs.edit().putString("saved_make", value).apply()

    var savedModel: String?
        get() = prefs.getString("saved_model", null)
        set(value) = prefs.edit().putString("saved_model", value).apply()

    var savedYear: Int
        get() = prefs.getInt("saved_year", 0)
        set(value) = prefs.edit().putInt("saved_year", value).apply()

    var connectionMode: String?
        get() = prefs.getString("connection_mode", null)
        set(value) = prefs.edit().putString("connection_mode", value).apply()

    var setupCompleted: Boolean
        get() = prefs.getBoolean("setup_completed", false)
        set(value) = prefs.edit().putBoolean("setup_completed", value).apply()

    fun hasVehicle(): Boolean = savedVin != null

    fun saveVehicle(vin: String, make: String?, model: String?, year: Int?) {
        savedVin = vin
        savedMake = make
        savedModel = model
        savedYear = year ?: 0
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
