package com.petfather.gyeonsimi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petfather.gyeonsimi.data.db.HealthRecord
import com.petfather.gyeonsimi.data.db.MealSlot
import com.petfather.gyeonsimi.data.db.Species
import com.petfather.gyeonsimi.domain.HealthAnalyzer
import com.petfather.gyeonsimi.ui.appViewModel
import com.petfather.gyeonsimi.ui.components.EmptyState
import com.petfather.gyeonsimi.ui.components.PetIcon
import com.petfather.gyeonsimi.ui.components.PetTopBar
import com.petfather.gyeonsimi.ui.components.SectionCard
import com.petfather.gyeonsimi.ui.components.StatusPill
import com.petfather.gyeonsimi.ui.components.ValueRow
import com.petfather.gyeonsimi.ui.theme.LocalStatusColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateTitle = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun HomeScreen(
    onPawClick: () -> Unit,
    onRegister: () -> Unit,
    onFeedList: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showEntry by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { PetTopBar(state.pet?.name ?: "견심이", onPawClick) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        HomeContent(
            state = state,
            contentPadding = padding,
            mapCard = { LocationCard() },
            onToggleMeal = vm::toggleMeal,
            onAddHealth = {
                if (state.canRecordHealth) {
                    showEntry = true
                } else {
                    scope.launch { snackbar.showSnackbar("먹이 정보를 먼저 입력해 주세요. 식사를 탭하면 기록돼요.") }
                }
            },
            onFeedList = onFeedList,
            onRegister = onRegister,
        )
    }

    val pet = state.pet
    if (showEntry && pet != null) {
        HealthEntrySheet(
            species = pet.species,
            initial = state.todayHealth,
            lastWeightKg = pet.weightKg,
            onDismiss = { showEntry = false },
            onSave = { input ->
                scope.launch {
                    val saved = vm.saveHealth(input)
                    showEntry = false
                    snackbar.showSnackbar(if (saved) "오늘의 건강 정보를 저장했어요." else "먹이 정보를 먼저 입력해 주세요.")
                }
            },
        )
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    contentPadding: PaddingValues,
    mapCard: @Composable () -> Unit,
    onToggleMeal: (MealSlot) -> Unit,
    onAddHealth: () -> Unit,
    onFeedList: () -> Unit,
    onRegister: () -> Unit,
) {
    val pet = state.pet
    if (!state.loading && pet == null) {
        Column(Modifier.fillMaxSize().padding(contentPadding), verticalArrangement = Arrangement.Center) {
            EmptyState(
                icon = PetIcon,
                title = "반려동물을 먼저 등록해 주세요",
                body = "아래 '등록' 탭이나 버튼을 눌러 강아지·고양이를 등록하면\n먹이·건강 기록을 시작할 수 있어요.",
                actionLabel = "반려동물 등록하기",
                onAction = onRegister,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { mapCard() }
        item { MealCard(state, onToggleMeal) }
        item {
            SectionCard(
                title = "사료 추천",
                subtitle = "추천 사료 확인",
                icon = Icons.Filled.SetMeal,
                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onFeedList,
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "인기 사료 10가지 추천",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { HealthCard(state, onAddHealth) }
    }
}

@Composable
private fun MealCard(state: HomeUiState, onToggleMeal: (MealSlot) -> Unit) {
    val status = LocalStatusColors.current
    val perMeal = state.feeding?.gramsPerMeal
    SectionCard(
        title = "먹이 정보",
        subtitle = if (perMeal != null) "오늘의 식사 · 끼니당 약 ${perMeal}g 권장" else "오늘의 식사",
        icon = Icons.Filled.Restaurant,
    ) {
        Spacer(Modifier.height(8.dp))
        MealSlot.entries.forEachIndexed { i, slot ->
            val log = state.meals[slot]
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(slot.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (log != null) {
                    val time = Instant.ofEpochMilli(log.loggedAt).atZone(ZoneId.systemDefault()).format(TimeFormat)
                    StatusPill("완료 $time", container = status.goodContainer, content = status.good, onClick = { onToggleMeal(slot) })
                } else {
                    StatusPill("대기 중", onClick = { onToggleMeal(slot) })
                }
            }
            if (i < MealSlot.entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
        Text(
            "식사를 마치면 '대기 중'을 탭해 주세요. 다시 탭하면 취소돼요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthCard(state: HomeUiState, onAddHealth: () -> Unit) {
    val record = state.todayHealth
    val species = state.pet?.species ?: Species.DOG
    SectionCard(
        title = "건강 정보",
        subtitle = state.today.format(DateTitle),
        icon = Icons.Filled.Favorite,
        trailing = {
            FilledTonalIconButton(
                onClick = onAddHealth,
                colors = if (state.canRecordHealth) {
                    IconButtonDefaults.filledTonalIconButtonColors()
                } else {
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = if (record == null) "오늘 건강 정보 입력" else "오늘 건강 정보 수정")
            }
        },
    ) {
        Spacer(Modifier.height(6.dp))
        if (record == null) {
            Text(
                if (state.canRecordHealth) "+ 버튼을 눌러 오늘의 체중·체온·심박수를 기록해 주세요."
                else "먹이 정보를 먼저 입력하면 건강 정보를 기록할 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            HealthValues(record, species)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthValues(record: HealthRecord, species: Species) {
    val tr = HealthAnalyzer.temperatureRange(species)
    val hr = HealthAnalyzer.heartRateRange(species)
    val warn = MaterialTheme.colorScheme.error
    ValueRow(Icons.Filled.MonitorWeight, "체중", record.weightKg?.let { "${HealthAnalyzer.fmt(it)}kg" } ?: "-")
    ValueRow(
        Icons.Filled.Thermostat, "체온",
        record.temperatureC?.let { "${HealthAnalyzer.fmt(it)}°C" } ?: "-",
        valueColor = record.temperatureC?.let { if (it in tr) Color.Unspecified else warn } ?: Color.Unspecified,
    )
    ValueRow(
        Icons.Filled.MonitorHeart, "심박수",
        record.heartRateBpm?.let { "${it}bpm" } ?: "-",
        valueColor = record.heartRateBpm?.let { if (it.toDouble() in hr) Color.Unspecified else warn } ?: Color.Unspecified,
    )
    if (record.tags.isNotEmpty()) {
        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            record.tags.forEach { tag ->
                val symptom = tag in HealthAnalyzer.SYMPTOM_TAGS
                StatusPill(
                    tag,
                    container = if (symptom) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    content = if (symptom) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
    if (record.memo.isNotBlank()) {
        Text(record.memo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal, modifier = Modifier.padding(top = 8.dp))
    }
}
