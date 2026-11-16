package com.petfather.gyeonsimi2.ui.community

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.petfather.gyeonsimi2.data.repo.CommunityRepository
import com.petfather.gyeonsimi2.ui.appViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritePostScreen(onDone: () -> Unit) {
    val vm = appViewModel { WritePostViewModel(it) }
    val scope = rememberCoroutineScope()
    var board by rememberSaveable { mutableStateOf(CommunityRepository.BOARDS.first()) }
    var content by rememberSaveable { mutableStateOf("") }
    var image by rememberSaveable { mutableStateOf<Uri?>(null) }
    var sending by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) image = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("글쓰기") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") } },
                actions = {
                    TextButton(
                        enabled = content.isNotBlank() && !sending,
                        onClick = {
                            sending = true
                            scope.launch {
                                vm.submit(board, content, image)
                                onDone()
                            }
                        },
                    ) { Text("등록") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CommunityRepository.BOARDS.forEach { b ->
                    FilterChip(selected = board == b, onClick = { board = b }, label = { Text(b) })
                }
            }
            OutlinedTextField(
                value = content,
                onValueChange = { content = it.take(1000) },
                placeholder = { Text("반려동물의 일상, 궁금한 점, 꿀팁을 나눠 주세요") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${content.length}/1000") },
            )
            val selected = image
            if (selected != null) {
                Box {
                    AsyncImage(
                        model = selected,
                        contentDescription = "선택한 사진",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(MaterialTheme.shapes.medium),
                    )
                    FilledIconButton(onClick = { image = null }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        Icon(Icons.Filled.Close, "사진 빼기")
                    }
                }
            } else {
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Filled.AddPhotoAlternate, null)
                    Text("  사진 추가")
                }
            }
        }
    }
}
