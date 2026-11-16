package com.petfather.gyeonsimi2.ui.register

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petfather.gyeonsimi2.data.db.Sex
import com.petfather.gyeonsimi2.data.db.Species
import com.petfather.gyeonsimi2.domain.BreedCatalog
import com.petfather.gyeonsimi2.domain.HealthAnalyzer
import com.petfather.gyeonsimi2.ui.appViewModel
import com.petfather.gyeonsimi2.ui.components.IconBadge
import com.petfather.gyeonsimi2.ui.components.PetIcon
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val BirthFormat = DateTimeFormatter.ofPattern("yyyy년 M월 d일")

@Composable
fun RegisterScreen(editId: Long?, onSaved: () -> Unit, onBack: (() -> Unit)?) {
    val vm = appViewModel(key = "register-${editId ?: "new"}") { RegisterViewModel(it, editId) }
    val form by vm.form.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    RegisterContent(
        form = form,
        today = vm.today(),
        onChange = vm::update,
        onSave = { scope.launch { if (vm.save()) onSaved() } },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterContent(
    form: PetForm,
    today: LocalDate,
    onChange: ((PetForm) -> PetForm) -> Unit,
    onSave: () -> Unit,
    onBack: (() -> Unit)?,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val editing = form.editingId != null
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(contentPadding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로") }
            } else {
                IconBadge(PetIcon, size = 48)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(if (editing) "반려동물 정보 수정" else "새 반려동물 등록", style = MaterialTheme.typography.titleLarge)
                Text("반려동물 정보를 입력해주세요", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        FormSection("기본 정보", Icons.Filled.Info) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Species.entries.forEachIndexed { i, s ->
                    SegmentedButton(
                        selected = form.species == s,
                        onClick = { onChange { it.copy(species = s, breed = if (it.species == s) it.breed else "") } },
                        shape = SegmentedButtonDefaults.itemShape(i, Species.entries.size),
                    ) { Text(s.label) }
                }
            }
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> onChange { it.copy(name = v.take(20)) } },
                label = { Text("이름") },
                leadingIcon = { Icon(PetIcon, null, tint = MaterialTheme.colorScheme.primary) },
                isError = form.showErrors && form.nameError,
                supportingText = if (form.showErrors && form.nameError) ({ Text("이름을 입력해 주세요") }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            BreedField(form, onChange)
            BirthDateRow(form.birthDate, today) { showDatePicker = true }
            OutlinedTextField(
                value = form.weight,
                onValueChange = { v -> onChange { it.copy(weight = v.replace(',', '.').filter { c -> c.isDigit() || c == '.' }.take(5)) } },
                label = { Text("몸무게 (kg)") },
                leadingIcon = { Icon(Icons.Filled.MonitorWeight, null) },
                isError = form.weightError,
                supportingText = if (form.weightError) ({ Text("0.1 ~ 120 사이로 입력해 주세요") }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FormSection("성별 및 중성화", Icons.Filled.FavoriteBorder) {
            Text("성별", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SexButton("수컷", Icons.Filled.Male, form.sex == Sex.MALE, Modifier.weight(1f)) { onChange { it.copy(sex = Sex.MALE) } }
                SexButton("암컷", Icons.Filled.Female, form.sex == Sex.FEMALE, Modifier.weight(1f)) { onChange { it.copy(sex = Sex.FEMALE) } }
            }
            if (form.showErrors && form.sexError) {
                Text("성별을 선택해 주세요", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Row(
                    Modifier.fillMaxWidth().clickable { onChange { it.copy(neutered = !it.neutered) } }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.MedicalServices, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("중성화 여부", style = MaterialTheme.typography.titleSmall)
                        Text("중성화 수술을 받았는지 선택하세요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = form.neutered, onCheckedChange = { v -> onChange { it.copy(neutered = v) } })
                }
            }
        }

        FormSection("건강 특성", Icons.Filled.HealthAndSafety) {
            OutlinedTextField(
                value = form.allergies,
                onValueChange = { v -> onChange { it.copy(allergies = v.take(60)) } },
                label = { Text("알레르기 (선택)") },
                placeholder = { Text("예: 닭고기, 곡물") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.conditions,
                onValueChange = { v -> onChange { it.copy(conditions = v.take(60)) } },
                label = { Text("기저질환 (선택)") },
                placeholder = { Text("예: 슬개골 탈구, 피부염") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (editing) "수정 완료" else "등록하기")
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        val zone = ZoneOffset.UTC
        val state = rememberDatePickerState(
            initialSelectedDateMillis = form.birthDate?.atStartOfDay(zone)?.toInstant()?.toEpochMilli(),
            yearRange = (today.year - 30)..today.year,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val date = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
                        if (!date.isAfter(today)) onChange { it.copy(birthDate = date) }
                    }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun FormSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreedField(form: PetForm, onChange: ((PetForm) -> PetForm) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val all = BreedCatalog.namesFor(form.species)
    val options = all.filter { form.breed.isBlank() || it.contains(form.breed.trim()) }.ifEmpty { all }
    val info = BreedCatalog.find(form.species, form.breed)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = form.breed,
            onValueChange = { v -> onChange { it.copy(breed = v.take(20)) }; expanded = true },
            label = { Text("품종 (선택)") },
            leadingIcon = { Icon(Icons.Filled.Category, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            supportingText = {
                Text(
                    if (info != null) "참고 성체 체중 ${HealthAnalyzer.fmt(info.minKg)} ~ ${HealthAnalyzer.fmt(info.maxKg)}kg"
                    else "목록에 없으면 직접 입력하세요 (믹스 등)",
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    onChange { it.copy(breed = name) }
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun BirthDateRow(date: LocalDate?, today: LocalDate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("생년월일", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    date?.let { "${it.format(BirthFormat)} (${HealthAnalyzer.ageLabel(it, today) ?: "-"})" } ?: "생년월일을 선택하세요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (date == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SexButton(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    if (selected) {
        Button(onClick = onClick, modifier = modifier.height(52.dp), shape = MaterialTheme.shapes.medium) {
            Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.height(52.dp), shape = MaterialTheme.shapes.medium) {
            Icon(icon, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant); Spacer(Modifier.width(6.dp))
            Text(label, color = colors.onSurface)
        }
    }
}
