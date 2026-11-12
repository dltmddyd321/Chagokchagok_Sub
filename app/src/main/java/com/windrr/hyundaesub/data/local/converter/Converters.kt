package com.windrr.hyundaesub.data.local.converter

import androidx.room.TypeConverter
import com.windrr.hyundaesub.data.local.entity.Orientation
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

    /** 허용 방향은 저장·조회 모두 정책대로 보정 (중복 제거, 비면 6방향) */
    @TypeConverter
    fun orientationsToJson(value: List<Orientation>): String =
        stringsToJson(Orientation.normalizeAllowed(value).map { it.name })

    @TypeConverter
    fun jsonToOrientations(value: String): List<Orientation> =
        Orientation.normalizeAllowed(jsonToStrings(value).mapNotNull { name -> Orientation.entries.find { it.name == name } })
}
