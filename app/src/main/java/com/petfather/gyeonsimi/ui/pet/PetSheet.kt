package com.petfather.gyeonsimi.ui.pet

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.petfather.gyeonsimi.AppContainer
import com.petfather.gyeonsimi.BuildConfig
import com.petfather.gyeonsimi.data.db.Pet
import com.petfather.gyeonsimi.data.repo.UserSettings
import com.petfather.gyeonsimi.domain.HealthAnalyzer
import com.petfather.gyeonsimi.notify.ReminderScheduler
import com.petfather.gyeonsimi.ui.appViewModel
import com.petfather.gyeonsimi.ui.components.IconBadge
import com.petfather.gyeonsimi.ui.components.PetIcon
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PetSheetState(val pets: List<Pet> = emptyList(), val activeId: Long? = null, val settings: UserSettings? = null)

class PetSheetViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<PetSheetState> = combine(c.pets.pets, c.pets.activePet, c.settings.settings) { pets, active, s ->
        PetSheetState(pets, active?.id, s)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetSheetState())

    fun today() = c.today()
    fun select(id: Long) = viewModelScope.launch { c.pets.select(id) }
    fun delete(pet: Pet) = viewModelScope.launch { c.pets.delete(pet) }
    fun setNickname(name: String) = viewModelScope.launch { c.settings.setNickname(name) }
    fun setReminder(enabled: Boolean) = viewModelScope.launch { c.settings.setReminderEnabled(enabled) }
}

/** 상단 발바닥 버튼: 반려동물 전환·수정·삭제와 앱 설정 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetSheet(onDismiss: () -> Unit, onAddPet: () -> Unit, onEditPet: (Long) -> Unit) {
    val vm = appViewModel { PetSheetViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleteTarget by remember { mutableStateOf<Pet?>(null) }
    var nickname by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.settings?.nickname) { if (nickname == null) nickname = state.settings?.nickname }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setReminder(granted)
        if (granted) ReminderScheduler.schedule(context)
    }

    // 시트를 어떤 방법으로 닫든 바뀐 닉네임을 저장합니다.
    fun saveNickname() {
        nickname?.let { if (it.isNotBlank() && it != state.settings?.nickname) vm.setNickname(it) }
    }

    ModalBottomSheet(onDismissRequest = { saveNickname(); onDismiss() }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("내 반려동물", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            state.pets.forEach { pet ->
                val active = pet.id == state.activeId
                Row(
                    Modifier.fillMaxWidth().clickable { saveNickname(); vm.select(pet.id); onDismiss() }.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(
                        PetIcon,
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pet.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(pet.species.label, pet.breed.ifBlank { null }, HealthAnalyzer.ageLabel(pet.birthDate, vm.today()), pet.sex.label)
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (active) Icon(Icons.Filled.CheckCircle, "선택됨", tint = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { saveNickname(); onEditPet(pet.id) }) { Icon(Icons.Filled.Edit, "${pet.name} 정보 수정") }
                    IconButton(onClick = { deleteTarget = pet }) { Icon(Icons.Filled.Delete, "${pet.name} 삭제") }
                }
            }
            Row(
                Modifier.fillMaxWidth().clickable { saveNickname(); onAddPet() }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(Icons.Filled.Add, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("새 반려동물 등록", style = MaterialTheme.typography.titleSmall)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("설정", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = nickname.orEmpty(),
                    onValueChange = { nickname = it.take(12) },
                    label = { Text("커뮤니티 닉네임") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.NotificationsActive, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("저녁 기록 알림", style = MaterialTheme.typography.titleSmall)
                    Text("매일 저녁 8시쯤, 오늘 기록이 비어 있으면 알려드려요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.settings?.reminderEnabled == true,
                    onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33) {
                            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.setReminder(on)
                            if (on) ReminderScheduler.schedule(context) else ReminderScheduler.cancel(context)
                        }
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "견심이 ${BuildConfig.VERSION_NAME} · 모든 기록은 이 기기에만 저장돼요.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }

    deleteTarget?.let { pet ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("${pet.name}을(를) 삭제할까요?") },
            text = { Text("먹이·건강 기록도 함께 삭제되며 되돌릴 수 없어요.") },
            confirmButton = { TextButton(onClick = { vm.delete(pet); deleteTarget = null }) { Text("삭제", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("취소") } },
            icon = { Icon(Icons.Filled.Delete, null) },
        )
    }
}

