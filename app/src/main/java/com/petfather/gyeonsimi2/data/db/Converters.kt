package com.petfather.gyeonsimi2.data.db

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun dateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun tagsToString(tags: List<String>): String = tags.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToTags(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}
