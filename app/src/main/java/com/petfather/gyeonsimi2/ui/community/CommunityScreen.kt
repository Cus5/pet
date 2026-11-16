package com.petfather.gyeonsimi2.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.petfather.gyeonsimi2.data.db.Post
import com.petfather.gyeonsimi2.data.db.PostWithCount
import com.petfather.gyeonsimi2.ui.appViewModel
import com.petfather.gyeonsimi2.ui.components.EmptyState
import com.petfather.gyeonsimi2.ui.components.StatusPill
import java.io.File

@Composable
fun CommunityScreen(onWrite: () -> Unit, onOpenPost: (Long) -> Unit) {
    val vm = appViewModel { CommunityViewModel(it) }
    val posts by vm.posts.collectAsStateWithLifecycle()
    CommunityContent(posts, onWrite, onOpenPost, vm::toggleLike, vm::delete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityContent(
    posts: List<PostWithCount>?,
    onWrite: () -> Unit,
    onOpenPost: (Long) -> Unit,
    onLike: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var deleteTarget by remember { mutableStateOf<Long?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("커뮤니티", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onWrite, icon = { Icon(Icons.Filled.Edit, null) }, text = { Text("글쓰기") })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        "베타 테스트 기간에는 글과 댓글이 이 기기에만 저장돼요. '예시' 표시 글은 앱 소개용 샘플이에요.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            if (posts != null && posts.isEmpty()) {
                item { EmptyState(Icons.Filled.Forum, "아직 글이 없어요", "반려동물의 일상이나 궁금한 점을 첫 글로 남겨 보세요.") }
            }
            items(posts.orEmpty(), key = { it.post.id }) { item ->
                PostItem(
                    post = item.post,
                    commentCount = item.commentCount,
                    onClick = { onOpenPost(item.post.id) },
                    onLike = { onLike(item.post.id) },
                    onDelete = { deleteTarget = item.post.id },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("글을 삭제할까요?") },
            text = { Text("삭제한 글과 댓글은 되돌릴 수 없어요.") },
            confirmButton = { TextButton(onClick = { onDelete(id); deleteTarget = null }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("취소") } },
        )
    }
}

@Composable
fun PostItem(
    post: Post,
    commentCount: Int,
    onClick: (() -> Unit)?,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    maxLines: Int = 6,
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 12.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(post.authorName)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(post.authorName, style = MaterialTheme.typography.titleSmall)
                    Text(" in ${post.board}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (post.isSample) {
                        Spacer(Modifier.width(6.dp))
                        StatusPill("예시")
                    }
                }
                Text(relativeTime(post.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (post.isMine) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "더보기") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("삭제") }, onClick = { menu = false; onDelete() })
                    }
                }
            }
        }
        post.imagePath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = "첨부 사진",
                contentScale = ContentScale.Crop,
                modifier = Modifier.padding(top = 10.dp).fillMaxWidth().aspectRatio(4f / 3f).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
        Text(
            post.content,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onLike) {
                Icon(
                    if (post.likedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (post.likedByMe) "좋아요 취소" else "좋아요",
                    tint = if (post.likedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("좋아요 ${post.likeCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier.clip(CircleShape).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Comment, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("댓글 $commentCount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun Avatar(name: String, size: Int = 40) {
    val palette = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
    )
    val (bg, fg) = palette[(name.hashCode() and 0x7fffffff) % palette.size]
    Box(Modifier.size(size.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text(name.take(1), color = fg, fontWeight = FontWeight.Bold)
    }
}

fun relativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - millis).coerceAtLeast(0) / 1000
    return when {
        diff < 60 -> "방금 전"
        diff < 3600 -> "${diff / 60}분 전"
        diff < 86_400 -> "${diff / 3600}시간 전"
        diff < 7 * 86_400 -> "${diff / 86_400}일 전"
        else -> java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy.M.d"))
    }
}
