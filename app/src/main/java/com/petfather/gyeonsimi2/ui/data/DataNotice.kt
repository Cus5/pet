package com.petfather.gyeonsimi2.ui.data

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.petfather.gyeonsimi2.AppContainer
import com.petfather.gyeonsimi2.ui.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DataNoticeViewModel(private val c: AppContainer) : ViewModel() {
    /** 서버가 설정된 빌드이고 아직 안내에 답하지 않았을 때만 true */
    val needsAnswer: StateFlow<Boolean> = c.settings.settings
        .map { c.telemetry.available && !it.dataNoticeAnswered }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun answer(usageStats: Boolean, shareWeights: Boolean) {
        viewModelScope.launch { c.telemetry.answerDataNotice(usageStats, shareWeights) }
    }
}

/** 첫 실행 때 한 번 보여 주는 안내. 둘 중 하나를 골라야 닫힙니다. */
@Composable
fun DataNoticeHost() {
    val vm = appViewModel { DataNoticeViewModel(it) }
    val needsAnswer by vm.needsAnswer.collectAsStateWithLifecycle()
    if (!needsAnswer) return
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            DataNoticeContent(onAnswer = vm::answer)
        }
    }
}

@Composable
fun DataNoticeContent(onAnswer: (usageStats: Boolean, shareWeights: Boolean) -> Unit) {
    var usageStats by rememberSaveable { mutableStateOf(true) }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("시작하기 전에", style = MaterialTheme.typography.titleLarge)
        NoticeRow(
            Icons.Filled.PhoneAndroid,
            "기록은 이 폰에 저장돼요",
            "회원가입이 없고, 반려동물 이름·메모·사진·위치는 서버로 보내지 않아요.",
        )
        Row(
            Modifier.fillMaxWidth().clickable { usageStats = !usageStats },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                NoticeRow(
                    Icons.Filled.Insights,
                    "익명 사용 통계 보내기",
                    "앱을 연 날과 어떤 기능을 썼는지만 보내요. 앱을 고치는 데 써요.",
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = usageStats, onCheckedChange = { usageStats = it })
        }
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.padding(14.dp)) {
                NoticeRow(
                    Icons.Filled.MonitorWeight,
                    "체중 기록을 익명으로 공유해 주실래요?",
                    "체중과 날짜, 반려동물의 종류·품종·태어난 달·성별·중성화 여부만 보내요. " +
                        "체중 변화를 예측하는 기능을 만드는 데 쓰고, 설정에서 언제든 끄거나 지울 수 있어요.",
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAnswer(usageStats, true) }, modifier = Modifier.fillMaxWidth()) {
                Text("체중 기록 공유하고 시작")
            }
            OutlinedButton(onClick = { onAnswer(usageStats, false) }, modifier = Modifier.fillMaxWidth()) {
                Text("공유 없이 시작")
            }
        }
    }
}

@Composable
private fun NoticeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
