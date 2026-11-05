package com.petfather.gyeonsimi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.petfather.gyeonsimi.data.db.HealthRecord
import com.petfather.gyeonsimi.data.db.Species
import com.petfather.gyeonsimi.domain.HealthAnalyzer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthEntrySheet(
    species: Species,
    initial: HealthRecord?,
    lastWeightKg: Double?,
    onDismiss: () -> Unit,
    onSave: (HealthInput) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        HealthEntryForm(species, initial, lastWeightKg, onDismiss, onSave)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthEntryForm(
    species: Species,
    initial: HealthRecord?,
    lastWeightKg: Double?,
    onCancel: () -> Unit,
    onSave: (HealthInput) -> Unit,
) {
    var weight by rememberSaveable { mutableStateOf(initial?.weightKg?.toString() ?: lastWeightKg?.toString().orEmpty()) }
    var temp by rememberSaveable { mutableStateOf(initial?.temperatureC?.toString().orEmpty()) }
    var heart by rememberSaveable { mutableStateOf(initial?.heartRateBpm?.toString().orEmpty()) }
    var memo by rememberSaveable { mutableStateOf(initial?.memo.orEmpty()) }
    val tags = remember { mutableStateListOf<String>().apply { addAll(initial?.tags.orEmpty()) } }

    val w = weight.toDoubleOrNull()
    val t = temp.toDoubleOrNull()
    val h = heart.toIntOrNull()
    val weightError = weight.isNotBlank() && (w == null || w !in 0.1..120.0)
    val tempError = temp.isNotBlank() && (t == null || t !in 30.0..45.0)
    val heartError = heart.isNotBlank() && (h == null || h !in 20..300)
    val empty = weight.isBlank() && temp.isBlank() && heart.isBlank() && tags.isEmpty()
    val tr = HealthAnalyzer.temperatureRange(species)
    val hr = HealthAnalyzer.heartRateRange(species)

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("오늘의 건강 정보", style = MaterialTheme.typography.titleLarge)
        Text(
            "측정한 항목만 입력해도 괜찮아요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NumberField("체중 (kg)", weight, { weight = it }, weightError, "0.1 ~ 120 사이로 입력해 주세요", "같은 저울로 같은 시간에 재면 정확해요")
        NumberField(
            "체온 (°C)", temp, { temp = it }, tempError, "30 ~ 45 사이로 입력해 주세요",
            "참고 범위 ${HealthAnalyzer.fmt(tr.min)} ~ ${HealthAnalyzer.fmt(tr.max)}°C (항문 체온계 기준)",
        )
        NumberField(
            "심박수 (bpm)", heart, { heart = it.filter(Char::isDigit) }, heartError, "20 ~ 300 사이로 입력해 주세요",
            "안정 시 15초간 센 횟수 × 4 · 참고 ${hr.min.toInt()} ~ ${hr.max.toInt()}bpm", KeyboardType.Number,
        )
        Text("건강 태그", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            HealthAnalyzer.ALL_TAGS.forEach { tag ->
                FilterChip(
                    selected = tag in tags,
                    onClick = { if (tag in tags) tags.remove(tag) else tags.add(tag) },
                    label = { Text(tag) },
                )
            }
        }
        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it.take(300) },
            label = { Text("메모 (선택)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("취소") }
            Spacer(Modifier.height(0.dp).padding(4.dp))
            Button(
                enabled = !weightError && !tempError && !heartError && !empty,
                onClick = { onSave(HealthInput(w, t, h, tags.toList(), memo.trim())) },
            ) { Text("저장") }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    isError: Boolean,
    errorText: String,
    helper: String,
    keyboard: KeyboardType = KeyboardType.Decimal,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)) },
        label = { Text(label) },
        isError = isError,
        supportingText = { Text(if (isError) errorText else helper) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}
