package com.petfather.gyeonsimi2.ui.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.petfather.gyeonsimi2.AppContainer
import com.petfather.gyeonsimi2.data.remote.FeedbackCategory
import com.petfather.gyeonsimi2.data.remote.SendResult
import com.petfather.gyeonsimi2.data.remote.Telemetry
import com.petfather.gyeonsimi2.ui.appViewModel
import kotlinx.coroutines.launch

class FeedbackViewModel(private val c: AppContainer) : ViewModel() {
    suspend fun send(category: FeedbackCategory, rating: Int?, message: String): SendResult =
        c.telemetry.sendFeedback(category, rating, message)
}

@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val vm = appViewModel { FeedbackViewModel(it) }
    FeedbackContent(onBack = onBack, onSend = vm::send)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackContent(
    onBack: () -> Unit,
    onSend: suspend (FeedbackCategory, Int?, String) -> SendResult,
    initialMessage: String = "",
    initialRating: Int = 0,
    initiallySent: Boolean = false,
) {
    var category by rememberSaveable { mutableStateOf(FeedbackCategory.IDEA) }
    var rating by rememberSaveable { mutableIntStateOf(initialRating) }
    var message by rememberSaveable { mutableStateOf(initialMessage) }
    var sending by rememberSaveable { mutableStateOf(false) }
    var sent by rememberSaveable { mutableStateOf(initiallySent) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("의견 보내기") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (sent) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Text("의견을 보냈어요", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                Text(
                    "보내 주신 의견은 견심이를 고치는 데 쓸게요. 고맙습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                Button(onClick = onBack) { Text("돌아가기") }
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("어떤 의견인가요?", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeedbackCategory.entries.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                }
            }
            Column {
                Text("견심이 만족도 (선택)", style = MaterialTheme.typography.titleSmall)
                Row {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = if (rating == star) 0 else star }) {
                            Icon(
                                if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "별 ${star}개",
                                tint = if (star <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(Telemetry.MAX_FEEDBACK_LENGTH) },
                label = { Text("내용") },
                placeholder = { Text("불편했던 점, 있으면 좋을 기능, 좋았던 점을 자유롭게 적어 주세요") },
                minLines = 6,
                supportingText = { Text("${message.length}/${Telemetry.MAX_FEEDBACK_LENGTH}") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "의견은 이름 없이 견심이 팀에게 전송돼요. 연락처 같은 개인정보는 적지 말아 주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                enabled = message.isNotBlank() && !sending,
                onClick = {
                    sending = true
                    scope.launch {
                        val result = onSend(category, rating.takeIf { it > 0 }, message)
                        sending = false
                        if (result == SendResult.SENT) {
                            sent = true
                        } else {
                            snackbar.showSnackbar("보내지 못했어요. 인터넷 연결을 확인하고 다시 시도해 주세요.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (sending) "보내는 중…" else "보내기") }
        }
    }
}
