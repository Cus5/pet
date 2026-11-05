package com.petfather.gyeonsimi.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY createdAt")
    fun observeAll(): Flow<List<Pet>>

    @Query("SELECT * FROM pets WHERE id = :id")
    suspend fun get(id: Long): Pet?

    @Insert
    suspend fun insert(pet: Pet): Long

    @Update
    suspend fun update(pet: Pet)

    @Delete
    suspend fun delete(pet: Pet)
}

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE petId = :petId AND date = :date")
    fun observeForDate(petId: Long, date: LocalDate): Flow<List<MealLog>>

    @Query("SELECT * FROM meals WHERE petId = :petId AND date >= :from ORDER BY date")
    fun observeSince(petId: Long, from: LocalDate): Flow<List<MealLog>>

    @Query("SELECT COUNT(*) FROM meals WHERE petId = :petId AND date = :date")
    suspend fun countForDate(petId: Long, date: LocalDate): Int

    @Upsert
    suspend fun upsert(meal: MealLog)

    @Query("DELETE FROM meals WHERE petId = :petId AND date = :date AND slot = :slot")
    suspend fun delete(petId: Long, date: LocalDate, slot: MealSlot)
}

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_records WHERE petId = :petId AND date = :date")
    fun observeForDate(petId: Long, date: LocalDate): Flow<HealthRecord?>

    @Query("SELECT * FROM health_records WHERE petId = :petId ORDER BY date")
    fun observeAll(petId: Long): Flow<List<HealthRecord>>

    @Query("SELECT * FROM health_records WHERE petId = :petId AND date = :date")
    suspend fun get(petId: Long, date: LocalDate): HealthRecord?

    @Upsert
    suspend fun upsert(record: HealthRecord)
}

@Dao
interface PostDao {
    @Query(
        """
        SELECT posts.*, (SELECT COUNT(*) FROM comments WHERE comments.postId = posts.id) AS commentCount
        FROM posts ORDER BY createdAt DESC
        """,
    )
    fun observeAll(): Flow<List<PostWithCount>>

    @Query("SELECT * FROM posts WHERE id = :id")
    fun observe(id: Long): Flow<Post?>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun get(id: Long): Post?

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int

    @Insert
    suspend fun insert(post: Post): Long

    @Update
    suspend fun update(post: Post)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt")
    fun observeForPost(postId: Long): Flow<List<Comment>>

    @Insert
    suspend fun insert(comment: Comment): Long
}
