package com.petfather.gyeonsimi.ui.community

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petfather.gyeonsimi.AppContainer
import com.petfather.gyeonsimi.data.db.Comment
import com.petfather.gyeonsimi.data.db.Post
import com.petfather.gyeonsimi.data.db.PostWithCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommunityViewModel(private val c: AppContainer) : ViewModel() {
    val posts: StateFlow<List<PostWithCount>?> =
        c.community.posts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleLike(id: Long) = viewModelScope.launch { c.community.toggleLike(id) }
    fun delete(id: Long) = viewModelScope.launch { c.community.delete(id) }
}

class WritePostViewModel(private val c: AppContainer) : ViewModel() {
    suspend fun submit(board: String, content: String, image: Uri?) = c.community.write(board, content, image)
}

class PostDetailViewModel(private val c: AppContainer, private val postId: Long) : ViewModel() {
    val post: StateFlow<Post?> = c.community.post(postId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val comments: StateFlow<List<Comment>> =
        c.community.comments(postId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleLike() = viewModelScope.launch { c.community.toggleLike(postId) }
    fun addComment(text: String) = viewModelScope.launch { c.community.addComment(postId, text) }
}
