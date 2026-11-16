package com.petfather.gyeonsimi2.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petfather.gyeonsimi2.domain.Alert
import com.petfather.gyeonsimi2.domain.HealthAnalyzer
import com.petfather.gyeonsimi2.domain.PetOverview
import com.petfather.gyeonsimi2.domain.Severity
import com.petfather.gyeonsimi2.domain.WeightStatus
import com.petfather.gyeonsimi2.ui.appViewModel
import com.petfather.gyeonsimi2.ui.components.EmptyState
import com.petfather.gyeonsimi2.ui.components.IconBadge
import com.petfather.gyeonsimi2.ui.components.PetIcon
import com.petfather.gyeonsimi2.ui.components.PetTopBar
import com.petfather.gyeonsimi2.ui.components.SectionCard
import com.petfather.gyeonsimi2.ui.components.StatusPill
import com.petfather.gyeonsimi2.ui.health.OverviewState
import com.petfather.gyeonsimi2.ui.health.OverviewViewModel
import com.petfather.gyeonsimi2.ui.theme.LocalStatusColors

@Composable
fun AiScreen(onPawClick: () -> Unit, onRegister: () -> Unit) {
    val vm = appViewModel(key = "ai") { OverviewViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val title = (state as? OverviewState.Ready)?.overview?.pet?.name ?: "AI 분석"
    Scaffold(topBar = { PetTopBar(title, onPawClick) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        when (val s = state) {
            OverviewState.Loading -> Unit
            OverviewState.NoPet -> EmptyState(
                PetIcon, "등록된 반려동물이 없어요", "반려동물을 등록하면 기록을 바탕으로\n건강 상태를 분석해 드려요.",
                Modifier.padding(padding), "반려동물 등록하기", onRegister,
            )
            is OverviewState.Ready -> AiContent(s.overview, padding)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiContent(overview: PetOverview, contentPadding: PaddingValues) {
    val report = overview.report
    val status = LocalStatusColors.current
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
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI 건강 분석", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        IconBadge(Icons.Filled.MonitorWeight, MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.colorScheme.primary, size = 48)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                report.chips.forEachIndexed { i, chip ->
                                    val first = i == 0
                                    val good = report.weightStatus == WeightStatus.NORMAL
                                    StatusPill(
                                        chip,
                                        container = when {
                                            !first -> MaterialTheme.colorScheme.surfaceContainerLowest
                                            good -> status.goodContainer
                                            report.weightStatus == WeightStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceContainerLowest
                                            else -> status.cautionContainer
                                        },
                                        content = when {
                                            !first -> MaterialTheme.colorScheme.onSurfaceVariant
                                            good -> status.good
                                            report.weightStatus == WeightStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> status.caution
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(report.headline, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        report.weightRange?.let { range ->
            item {
                val current = report.currentWeightKg
                SectionCard(title = "추천 체중 범위", icon = Icons.AutoMirrored.Filled.ShowChart) {
                    Spacer(Modifier.height(14.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("현재", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(current?.let { "${HealthAnalyzer.fmt(it)}kg" } ?: "-", style = MaterialTheme.typography.titleMedium)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(if (range.basedOnBreed) "권장 (${overview.pet.breed})" else "권장 (등록 체중 ±10%)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${HealthAnalyzer.fmt(range.minKg)} ~ ${HealthAnalyzer.fmt(range.maxKg)}kg", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (current != null) WeightGauge(current, range.minKg, range.maxKg)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when (report.weightStatus) {
                            WeightStatus.NORMAL -> "현재 체중이 권장 범위 안에 있어요. 주 1회 정도 체중을 기록해 안정적인 패턴을 유지해 보세요."
                            WeightStatus.OVER -> "권장 범위보다 무거워요. 급여량을 줄이고 활동량을 조금씩 늘려 보세요."
                            WeightStatus.UNDER -> if (overview.stage == com.petfather.gyeonsimi2.domain.LifeStage.BABY) "아직 성장 중이라 성체 기준보다 가벼운 것이 자연스러워요." else "권장 범위보다 가벼워요. 식사량을 확인해 주세요."
                            WeightStatus.UNKNOWN -> "체중을 기록하면 범위와 비교해 드려요."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        report.feeding?.let { plan ->
            item {
                SectionCard(
                    title = "적정 급여량",
                    subtitle = "건사료 기준 · 하루 ${plan.mealsPerDay}끼",
                    icon = Icons.Filled.Restaurant,
                    iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Spacer(Modifier.height(14.dp))
                    Row {
                        Metric("하루 열량", "${plan.kcalPerDay}kcal", Modifier.weight(1f))
                        Metric("하루 급여량", "약 ${plan.gramsPerDay}g", Modifier.weight(1f))
                        Metric("끼니당", "약 ${plan.gramsPerMeal}g", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(plan.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (report.alerts.isNotEmpty()) {
            item {
                SectionCard(
                    title = "주의 신호",
                    subtitle = "최근 기록에서 발견한 내용",
                    icon = Icons.Filled.ReportProblem,
                    iconContainer = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Spacer(Modifier.height(8.dp))
                    report.alerts.forEach { AlertRow(it) }
                }
            }
        }

        item {
            SectionCard(title = "AI 건강 인사이트", icon = Icons.Filled.Lightbulb) {
                Spacer(Modifier.height(8.dp))
                report.insights.forEach { line ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Row(Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "입력한 기록을 일반적인 수의학 참고 기준과 비교해 자동으로 만든 안내예요. 진단이 아니며, 이상이 느껴지면 꼭 동물병원에 문의하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** 권장 범위를 가운데 두고 현재 체중 위치를 보여주는 막대 */
@Composable
private fun WeightGauge(current: Double, min: Double, max: Double) {
    val span = max - min
    val lo = min - span * 0.5
    val hi = max + span * 0.5
    val pos = ((current - lo) / (hi - lo)).coerceIn(0.0, 1.0).toFloat()
    val startFrac = ((min - lo) / (hi - lo)).toFloat()
    val endFrac = ((max - lo) / (hi - lo)).toFloat()
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().height(14.dp)) {
        Box(Modifier.fillMaxWidth().height(8.dp).align(Alignment.Center).clip(CircleShape).background(colors.surfaceContainerHighest))
        Row(Modifier.fillMaxWidth().height(8.dp).align(Alignment.Center)) {
            Spacer(Modifier.weight(startFrac.coerceAtLeast(0.001f)))
            Box(Modifier.weight((endFrac - startFrac).coerceAtLeast(0.001f)).height(8.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.35f)))
            Spacer(Modifier.weight((1f - endFrac).coerceAtLeast(0.001f)))
        }
        Row(Modifier.fillMaxWidth().align(Alignment.Center)) {
            Spacer(Modifier.weight(pos.coerceAtLeast(0.001f)))
            Box(Modifier.size(14.dp).clip(CircleShape).background(colors.primary))
            Spacer(Modifier.weight((1f - pos).coerceAtLeast(0.001f)))
        }
    }
}

@Composable
private fun AlertRow(alert: Alert) {
    val status = LocalStatusColors.current
    val (icon, tint) = when (alert.severity) {
        Severity.WARNING -> Icons.Filled.WarningAmber to MaterialTheme.colorScheme.error
        Severity.CAUTION -> Icons.Filled.WarningAmber to status.caution
        Severity.INFO -> Icons.Filled.Info to MaterialTheme.colorScheme.primary
    }
    Row(Modifier.padding(vertical = 7.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(alert.title, style = MaterialTheme.typography.titleSmall, color = tint)
            Text(alert.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
