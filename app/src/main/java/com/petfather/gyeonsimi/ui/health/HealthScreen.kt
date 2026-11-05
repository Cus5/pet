package com.petfather.gyeonsimi.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.petfather.gyeonsimi.AppContainer
import com.petfather.gyeonsimi.data.db.HealthRecord
import com.petfather.gyeonsimi.domain.HealthAnalyzer
import com.petfather.gyeonsimi.domain.PetOverview
import com.petfather.gyeonsimi.domain.petOverview
import com.petfather.gyeonsimi.ui.appViewModel
import com.petfather.gyeonsimi.ui.components.ChartPoint
import com.petfather.gyeonsimi.ui.components.EmptyState
import com.petfather.gyeonsimi.ui.components.LineChart
import com.petfather.gyeonsimi.ui.components.PetIcon
import com.petfather.gyeonsimi.ui.components.PetTopBar
import com.petfather.gyeonsimi.ui.components.SectionCard
import com.petfather.gyeonsimi.ui.components.StatusPill
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.temporal.ChronoUnit

sealed interface OverviewState {
    data object Loading : OverviewState
    data object NoPet : OverviewState
    data class Ready(val overview: PetOverview) : OverviewState
}

class OverviewViewModel(c: AppContainer) : ViewModel() {
    val state: StateFlow<OverviewState> = petOverview(c.pets, c.care, c::today)
        .map { if (it == null) OverviewState.NoPet else OverviewState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewState.Loading)
}

enum class Period(val label: String, val days: Long?) { WEEK("7일", 7), MONTH("30일", 30), ALL("전체", null) }

@Composable
fun HealthScreen(onPawClick: () -> Unit, onRegister: () -> Unit) {
    val vm = appViewModel { OverviewViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val title = (state as? OverviewState.Ready)?.overview?.pet?.name ?: "건강"
    Scaffold(topBar = { PetTopBar(title, onPawClick) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        when (val s = state) {
            OverviewState.Loading -> Unit
            OverviewState.NoPet -> EmptyState(
                PetIcon, "등록된 반려동물이 없어요", "반려동물을 등록하고 건강 정보를 기록하면\n여기에서 그래프로 볼 수 있어요.",
                Modifier.padding(padding), "반려동물 등록하기", onRegister,
            )
            is OverviewState.Ready -> HealthContent(s.overview, padding)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HealthContent(overview: PetOverview, contentPadding: PaddingValues, initialPeriod: Period = Period.MONTH) {
    var period by rememberSaveable { mutableStateOf(initialPeriod) }
    val days = period.days
    val records = overview.records.filter { r ->
        days == null || ChronoUnit.DAYS.between(r.date, overview.today) in 0 until days
    }
    val species = overview.pet.species

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Period.entries.forEachIndexed { i, p ->
                    SegmentedButton(
                        selected = p == period,
                        onClick = { period = p },
                        shape = SegmentedButtonDefaults.itemShape(i, Period.entries.size),
                    ) { Text(p.label) }
                }
            }
        }
        if (overview.records.isEmpty()) {
            item {
                EmptyState(
                    Icons.Filled.Insights,
                    "아직 건강 기록이 없어요",
                    "홈 화면에서 먹이 정보를 입력한 뒤\n건강 정보 + 버튼으로 오늘의 기록을 남겨 주세요.",
                )
            }
            return@LazyColumn
        }
        item { TagCard(records) }
        item {
            val pts = records.mapNotNull { r -> r.weightKg?.let { ChartPoint(r.date, it) } }
            val range = overview.report.weightRange
            SectionCard(
                title = "체중",
                subtitle = overview.report.currentWeightKg?.let { "최근: ${HealthAnalyzer.fmt(it)}kg" } ?: "기록 없음",
                icon = Icons.Filled.MonitorWeight,
            ) {
                ChartOrHint(pts, range?.let { it.minKg..it.maxKg }, "kg")
            }
        }
        item {
            Text("기타 건강 정보", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
        }
        item {
            val pts = records.mapNotNull { r -> r.temperatureC?.let { ChartPoint(r.date, it) } }
            val tr = HealthAnalyzer.temperatureRange(species)
            SectionCard(
                title = "체온",
                subtitle = "정상 참고 범위 ${HealthAnalyzer.fmt(tr.min)} ~ ${HealthAnalyzer.fmt(tr.max)}°C",
                icon = Icons.Filled.Thermostat,
            ) { ChartOrHint(pts, tr.min..tr.max, "°C") }
        }
        item {
            val pts = records.mapNotNull { r -> r.heartRateBpm?.let { ChartPoint(r.date, it.toDouble()) } }
            val hr = HealthAnalyzer.heartRateRange(species)
            SectionCard(
                title = "심박수",
                subtitle = "정상 참고 범위 ${hr.min.toInt()} ~ ${hr.max.toInt()}bpm",
                icon = Icons.Filled.MonitorHeart,
            ) { ChartOrHint(pts, hr.min..hr.max, "bpm") }
        }
    }
}

@Composable
private fun ChartOrHint(points: List<ChartPoint>, range: ClosedFloatingPointRange<Double>?, unit: String) {
    Spacer(Modifier.height(12.dp))
    if (points.isEmpty()) {
        Text(
            "이 기간에 기록이 없어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    } else {
        LineChart(points, normalRange = range, unit = unit)
        Text(
            "점선 사이 옅은 영역이 참고 범위예요.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCard(records: List<HealthRecord>) {
    val counts = records.flatMap { it.tags }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
    SectionCard(title = "건강 태그", subtitle = "기간 내 기록된 태그와 횟수", icon = Icons.AutoMirrored.Filled.Label) {
        Spacer(Modifier.height(10.dp))
        if (counts.isEmpty()) {
            Text("기록된 태그가 없어요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                counts.forEach { (tag, n) ->
                    val symptom = tag in HealthAnalyzer.SYMPTOM_TAGS
                    StatusPill(
                        if (n > 1) "$tag ×$n" else tag,
                        container = if (symptom) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        content = if (symptom) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}
