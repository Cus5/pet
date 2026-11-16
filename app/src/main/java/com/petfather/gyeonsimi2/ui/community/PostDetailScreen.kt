package com.petfather.gyeonsimi2.ui.community

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petfather.gyeonsimi2.ui.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(postId: Long, onBack: () -> Unit) {
    val vm = appViewModel(key = "post-$postId") { PostDetailViewModel(it, postId) }
    val post by vm.post.collectAsStateWithLifecycle()
    val comments by vm.comments.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(post?.board ?: "게시글") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(300) },
                        placeholder = { Text("댓글을 남겨 주세요") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                    )
                    IconButton(enabled = text.isNotBlank(), onClick = { vm.addComment(text); text = "" }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "댓글 등록", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
    ) { padding ->
        val p = post
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            if (p != null) {
                item {
                    PostItem(p, comments.size, onClick = null, onLike = vm::toggleLike, onDelete = {}, maxLines = Int.MAX_VALUE)
                    HorizontalDivider()
                }
            }
            if (comments.isEmpty()) {
                item {
                    Text(
                        "첫 댓글을 남겨 보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(comments, key = { it.id }) { c ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Avatar(c.authorName, size = 32)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.authorName, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(6.dp))
                            Text(relativeTime(c.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(c.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
