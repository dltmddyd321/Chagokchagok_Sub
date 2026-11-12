package com.windrr.hyundaesub.data.local.converter

import androidx.room.TypeConverter
import com.windrr.hyundaesub.data.local.entity.SeatCode
import org.json.JSONArray

/** 리스트 컬럼은 json 배열 문자열로 저장 (ERD의 json 타입) */
class Converters {

    @TypeConverter
    fun stringsToJson(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun jsonToStrings(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { array.getString(it) }
    }

    @TypeConverter
    fun seatsToJson(value: List<SeatCode>): String = stringsToJson(value.map { it.name })

    @TypeConverter
    fun jsonToSeats(value: String): List<SeatCode> = jsonToStrings(value).map { SeatCode.valueOf(it) }
}
