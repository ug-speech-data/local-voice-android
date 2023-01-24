package com.hrd.localvoice.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StringListConvector {
    val gson = Gson()
    @TypeConverter
    fun fromString(value: String): List<String>? {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type);
    }

    @TypeConverter
    fun toStringList(data: List<String>?): String? {
        return gson.toJson(data);
    }
}