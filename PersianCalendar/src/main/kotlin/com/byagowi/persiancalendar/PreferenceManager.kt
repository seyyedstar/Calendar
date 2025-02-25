package com.byagowi.persiancalendar

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    fun saveInt(key: String, value: Int=0) {
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getInt(key: String, defaultValue: Int=0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun saveBoolean(key: String , value: Boolean=false){
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }
    fun getBoolean(key:String , defaultValue: Boolean=false):Boolean{
        return sharedPreferences.getBoolean(key, defaultValue)
    }
}
