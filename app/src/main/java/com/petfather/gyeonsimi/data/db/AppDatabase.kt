package com.petfather.gyeonsimi.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Pet::class, MealLog::class, HealthRecord::class, Post::class, Comment::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun mealDao(): MealDao
    abstract fun healthDao(): HealthDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "gyeonsimi.db").build()
    }
}
