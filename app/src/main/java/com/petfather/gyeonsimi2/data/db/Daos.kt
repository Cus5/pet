package com.petfather.gyeonsimi2.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    /** 체중이 있고, 아직 보내지 않았거나 보낸 뒤 고친 기록 */
    @Query("SELECT * FROM health_records WHERE weightKg IS NOT NULL AND (uploadedAt IS NULL OR uploadedAt < updatedAt) ORDER BY date LIMIT :limit")
    suspend fun weightsToUpload(limit: Int): List<HealthRecord>

    /** 보내는 사이에 기록이 또 바뀌었다면(updatedAt 이 다르면) 표시하지 않아 다음에 다시 보냅니다. */
    @Query("UPDATE health_records SET uploadedAt = :now WHERE petId = :petId AND date = :date AND updatedAt = :updatedAt")
    suspend fun markUploaded(petId: Long, date: LocalDate, updatedAt: Long, now: Long)

    @Query("UPDATE health_records SET uploadedAt = NULL")
    suspend fun clearUploaded()
}

@Dao
interface UsageEventDao {
    /** 같은 날 같은 이벤트가 이미 있으면 무시합니다. 새로 넣었으면 줄 번호(>0)를 돌려줍니다. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: LocalUsageEvent): Long

    @Query("SELECT * FROM usage_events WHERE sentAt IS NULL ORDER BY day")
    suspend fun unsent(): List<LocalUsageEvent>

    @Query("UPDATE usage_events SET sentAt = :now WHERE day = :day AND name = :name")
    suspend fun markSent(day: LocalDate, name: String, now: Long)

    @Query("DELETE FROM usage_events WHERE day < :before")
    suspend fun deleteOlderThan(before: LocalDate)

    @Query("DELETE FROM usage_events WHERE sentAt IS NULL")
    suspend fun deleteUnsent()
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
