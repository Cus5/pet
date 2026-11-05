package com.petfather.gyeonsimi.data.repo

import android.content.Context
import android.net.Uri
import com.petfather.gyeonsimi.data.db.Comment
import com.petfather.gyeonsimi.data.db.CommentDao
import com.petfather.gyeonsimi.data.db.Post
import com.petfather.gyeonsimi.data.db.PostDao
import com.petfather.gyeonsimi.data.db.PostWithCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 커뮤니티 글/댓글.
 * 베타 버전에서는 서버 없이 이 기기 안에만 저장합니다. 서버(Firebase 등)를 붙일 때는
 * 이 클래스만 바꾸면 화면 코드는 그대로 쓸 수 있습니다.
 */
class CommunityRepository(
    private val context: Context,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val settings: SettingsRepository,
) {
    val posts: Flow<List<PostWithCount>> = postDao.observeAll()

    fun post(id: Long): Flow<Post?> = postDao.observe(id)

    fun comments(postId: Long): Flow<List<Comment>> = commentDao.observeForPost(postId)

    suspend fun write(board: String, content: String, image: Uri?) {
        val nickname = settings.current().nickname
        val path = image?.let { copyImage(it) }
        postDao.insert(
            Post(authorName = nickname, board = board, content = content.trim(), imagePath = path, isMine = true),
        )
    }

    suspend fun toggleLike(postId: Long) {
        val post = postDao.get(postId) ?: return
        val liked = !post.likedByMe
        postDao.update(post.copy(likedByMe = liked, likeCount = (post.likeCount + if (liked) 1 else -1).coerceAtLeast(0)))
    }

    suspend fun delete(postId: Long) {
        postDao.get(postId)?.imagePath?.let { File(it).delete() }
        postDao.delete(postId)
    }

    suspend fun addComment(postId: Long, content: String) {
        commentDao.insert(Comment(postId = postId, authorName = settings.current().nickname, content = content.trim()))
    }

    /** 처음 설치했을 때 커뮤니티가 비어 보이지 않도록 '예시' 표시가 붙은 글을 넣습니다. */
    suspend fun seedSamplesIfNeeded() {
        if (!settings.markSamplesSeeded() || postDao.count() > 0) return
        val now = System.currentTimeMillis()
        val p1 = postDao.insert(
            Post(
                authorName = "민수",
                board = BOARDS[0],
                content = "우리 뽀삐 오늘 산책 나갔어요! 너무 좋아하네요 🐕",
                createdAt = now - 3 * 60_000,
                likeCount = 24,
                isSample = true,
            ),
        )
        commentDao.insert(Comment(postId = p1, authorName = "지은", content = "뽀삐 산책 최고! 날씨 좋을 때 많이 걸어요 :)", createdAt = now - 60_000))
        postDao.insert(
            Post(
                authorName = "지은",
                board = BOARDS[1],
                content = "고양이 사료 추천해주세요! 6개월 된 페르시안 키우고 있어요. 어떤 브랜드가 좋을까요?",
                createdAt = now - 60 * 60_000,
                likeCount = 8,
                isSample = true,
            ),
        )
        postDao.insert(
            Post(
                authorName = "견심이 팀",
                board = BOARDS[2],
                content = "견심이 베타 테스트에 참여해 주셔서 감사해요! 불편한 점이나 바라는 기능을 댓글로 남겨 주세요.",
                createdAt = now - 3 * 60 * 60_000,
                likeCount = 3,
                isSample = true,
            ),
        )
    }

    private suspend fun copyImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "images").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            } ?: return@runCatching null
            file.absolutePath
        }.getOrNull()
    }

    companion object {
        val BOARDS = listOf("강아지 사랑방", "고양이 집사들", "질문·답변")
    }
}
