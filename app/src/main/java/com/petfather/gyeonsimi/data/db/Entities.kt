package com.petfather.gyeonsimi.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Species(val label: String) { DOG("강아지"), CAT("고양이") }

enum class Sex(val label: String) { MALE("수컷"), FEMALE("암컷") }

@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: Species,
    val breed: String,
    val birthDate: LocalDate?,
    val sex: Sex,
    val neutered: Boolean,
    /** 등록할 때 입력한 몸무게. 건강 기록이 없을 때 기준값으로 씁니다. */
    val weightKg: Double?,
    val allergies: String = "",
    val conditions: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

enum class MealSlot(val label: String) { BREAKFAST("아침"), LUNCH("점심"), DINNER("저녁") }

@Entity(
    tableName = "meals",
    primaryKeys = ["petId", "date", "slot"],
    foreignKeys = [
        ForeignKey(entity = Pet::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class MealLog(
    val petId: Long,
    val date: LocalDate,
    val slot: MealSlot,
    val loggedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "health_records",
    primaryKeys = ["petId", "date"],
    foreignKeys = [
        ForeignKey(entity = Pet::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class HealthRecord(
    val petId: Long,
    val date: LocalDate,
    val weightKg: Double?,
    val temperatureC: Double?,
    val heartRateBpm: Int?,
    val tags: List<String>,
    val memo: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorName: String,
    val board: String,
    val content: String,
    /** 앱 내부 저장소에 복사한 사진 파일 경로 */
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
    /** 처음 설치 시 넣어 두는 예시 글 여부 */
    val isSample: Boolean = false,
    val isMine: Boolean = false,
)

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(entity = Post::class, parentColumns = ["id"], childColumns = ["postId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("postId")],
)
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val authorName: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class PostWithCount(
    @Embedded val post: Post,
    val commentCount: Int,
)
