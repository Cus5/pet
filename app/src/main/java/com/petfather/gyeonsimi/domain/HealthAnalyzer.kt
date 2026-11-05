package com.petfather.gyeonsimi.domain

import com.petfather.gyeonsimi.data.db.HealthRecord
import com.petfather.gyeonsimi.data.db.MealLog
import com.petfather.gyeonsimi.data.db.Pet
import com.petfather.gyeonsimi.data.db.Species
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

enum class LifeStage(val label: String) { BABY("어린 시기"), ADULT("성체"), SENIOR("노령") }

enum class WeightStatus(val label: String) { UNDER("저체중 주의"), NORMAL("안정 체중"), OVER("과체중 주의"), UNKNOWN("체중 기록 필요") }

enum class Severity { INFO, CAUTION, WARNING }

data class Alert(val severity: Severity, val title: String, val body: String)

data class Range(val min: Double, val max: Double) {
    operator fun contains(v: Double) = v in min..max
}

data class WeightRange(val minKg: Double, val maxKg: Double, val basedOnBreed: Boolean)

data class FeedingPlan(
    val kcalPerDay: Int,
    val gramsPerDay: Int,
    val gramsPerMeal: Int,
    val mealsPerDay: Int,
    val note: String,
)

data class HealthReport(
    val chips: List<String>,
    val headline: String,
    val weightStatus: WeightStatus,
    val currentWeightKg: Double?,
    val weightRange: WeightRange?,
    val feeding: FeedingPlan?,
    val alerts: List<Alert>,
    val insights: List<String>,
)

/**
 * 입력된 기록을 수의학 참고 기준과 비교해 상태를 요약하는 규칙 기반 분석기.
 * 진단 도구가 아니며, 화면에는 항상 '수의사 상담' 안내를 함께 보여줍니다.
 */
object HealthAnalyzer {

    /** 건강 태그 중 반복되면 병원 상담을 권하는 증상 */
    val SYMPTOM_TAGS = setOf("설사", "구토", "혈변", "기침", "식욕 저하", "무기력", "절뚝거림", "가려움")

    /** 기록 화면에서 고를 수 있는 건강 태그 */
    val ALL_TAGS = listOf("배변", "눈곱") + SYMPTOM_TAGS.toList() + listOf("산책", "목욕", "양치")

    fun temperatureRange(species: Species) = when (species) {
        Species.DOG -> Range(37.5, 39.2)
        Species.CAT -> Range(38.0, 39.2)
    }

    fun heartRateRange(species: Species) = when (species) {
        Species.DOG -> Range(60.0, 160.0)
        Species.CAT -> Range(140.0, 220.0)
    }

    fun ageInMonths(birth: LocalDate?, today: LocalDate): Int? =
        birth?.let { ChronoUnit.MONTHS.between(it, today).toInt().coerceAtLeast(0) }

    fun ageLabel(birth: LocalDate?, today: LocalDate): String? {
        birth ?: return null
        val p = Period.between(birth, today)
        return when {
            p.isNegative -> null
            p.years >= 1 -> "${p.years}세"
            p.months >= 1 -> "${p.months}개월"
            else -> "${p.days.coerceAtLeast(0)}일"
        }
    }

    fun lifeStage(species: Species, months: Int?): LifeStage = when {
        months == null -> LifeStage.ADULT
        months < 12 -> LifeStage.BABY
        species == Species.DOG && months >= 84 -> LifeStage.SENIOR
        species == Species.CAT && months >= 120 -> LifeStage.SENIOR
        else -> LifeStage.ADULT
    }

    fun weightRange(pet: Pet, baselineKg: Double?): WeightRange? {
        BreedCatalog.find(pet.species, pet.breed)?.let { return WeightRange(it.minKg, it.maxKg, basedOnBreed = true) }
        val base = baselineKg ?: return null
        return WeightRange(round1(base * 0.9), round1(base * 1.1), basedOnBreed = false)
    }

    fun weightStatus(current: Double?, range: WeightRange?): WeightStatus = when {
        current == null || range == null -> WeightStatus.UNKNOWN
        current < range.minKg -> WeightStatus.UNDER
        current > range.maxKg -> WeightStatus.OVER
        else -> WeightStatus.NORMAL
    }

    /**
     * 휴식 에너지 요구량(RER = 70 × 체중^0.75)에 생애 단계·중성화·체중 상태 계수를 곱해
     * 하루 권장 열량을 구하고, 건사료 열량(기본 3.7kcal/g)으로 급여량을 계산합니다.
     */
    fun feedingPlan(
        pet: Pet,
        weightKg: Double?,
        months: Int?,
        status: WeightStatus,
        range: WeightRange?,
        kcalPerGram: Double = 3.7,
        mealsPerDay: Int = 3,
    ): FeedingPlan? {
        val current = weightKg ?: return null
        if (current <= 0) return null
        // 체중 감량/증량이 필요하면 목표 체중(권장 범위 경계) 기준으로 계산
        val target = when (status) {
            WeightStatus.OVER -> range?.maxKg ?: current
            WeightStatus.UNDER -> range?.minKg ?: current
            else -> current
        }
        val rer = 70 * target.pow(0.75)
        val stage = lifeStage(pet.species, months)
        val factor = when (pet.species) {
            Species.DOG -> when {
                stage == LifeStage.BABY && (months ?: 12) < 4 -> 3.0
                stage == LifeStage.BABY -> 2.0
                status == WeightStatus.OVER -> 1.0
                status == WeightStatus.UNDER -> 1.8
                stage == LifeStage.SENIOR -> 1.4
                pet.neutered -> 1.6
                else -> 1.8
            }
            Species.CAT -> when {
                stage == LifeStage.BABY -> 2.5
                status == WeightStatus.OVER -> 0.8
                status == WeightStatus.UNDER -> 1.4
                stage == LifeStage.SENIOR -> 1.1
                pet.neutered -> 1.2
                else -> 1.4
            }
        }
        val kcal = (rer * factor).roundToInt()
        val grams = (kcal / kcalPerGram).roundToInt()
        val note = when (status) {
            WeightStatus.OVER -> "목표 체중 ${fmt(target)}kg 기준 감량 급여량이에요."
            WeightStatus.UNDER -> "목표 체중 ${fmt(target)}kg 기준 증량 급여량이에요."
            else -> "현재 체중 기준이에요."
        } + " 사료 포장지의 열량(kcal/g)에 따라 조절해 주세요."
        return FeedingPlan(kcal, grams, (grams.toDouble() / mealsPerDay).roundToInt(), mealsPerDay, note)
    }

    fun analyze(
        pet: Pet,
        records: List<HealthRecord>,
        meals: List<MealLog>,
        today: LocalDate,
    ): HealthReport {
        val sorted = records.sortedBy { it.date }
        val weighed = sorted.filter { it.weightKg != null }
        val currentWeight = weighed.lastOrNull()?.weightKg ?: pet.weightKg
        val baseline = pet.weightKg ?: weighed.firstOrNull()?.weightKg
        val months = ageInMonths(pet.birthDate, today)
        val stage = lifeStage(pet.species, months)
        val range = weightRange(pet, baseline)
        val status = weightStatus(currentWeight, range)
        val feeding = feedingPlan(pet, currentWeight, months, status, range)

        val alerts = buildList {
            addAll(vitalAlerts(pet, sorted.lastOrNull(), today))
            weightChangeAlert(weighed, today)?.let(::add)
            addAll(symptomAlerts(sorted, today))
            // 어린 시기에는 성체 기준보다 가벼운 것이 정상이라 저체중 경고를 띄우지 않습니다.
            val growing = stage == LifeStage.BABY && status == WeightStatus.UNDER
            if (status == WeightStatus.OVER) {
                add(Alert(Severity.CAUTION, "체중이 권장 범위보다 높아요", "간식을 줄이고 산책 시간을 조금씩 늘려 보세요. 급여량 카드를 참고하세요."))
            } else if (status == WeightStatus.UNDER && !growing) {
                add(Alert(Severity.CAUTION, "체중이 권장 범위보다 낮아요", "식사량이 충분한지 확인하고, 계속 빠진다면 병원 상담을 권해요."))
            }
        }

        val chips = buildList {
            add(if (stage == LifeStage.BABY && status == WeightStatus.UNDER) "성장 중" else status.label)
            val age = ageLabel(pet.birthDate, today)
            add(listOfNotNull(age, pet.sex.label).joinToString(" · "))
            if (pet.breed.isNotBlank()) add(pet.breed)
        }

        val headline = when {
            alerts.any { it.severity == Severity.WARNING } ->
                "${pet.name}에게 확인이 필요한 신호가 있어요. 아래 '주의 신호'를 확인하고 필요하면 동물병원에 문의해 주세요."
            status == WeightStatus.NORMAL && alerts.isEmpty() ->
                "좋아요! ${pet.name}의 체중은 권장 범위 안에 있고, 최근 기록에서 특별한 이상 신호는 없어요. 지금 루틴을 유지해 주세요."
            status == WeightStatus.UNKNOWN ->
                "${pet.name}의 체중을 기록하면 권장 체중 범위와 급여량을 알려드릴게요."
            alerts.isEmpty() ->
                "${pet.name}의 체중이 ${status.label.removeSuffix(" 주의")} 쪽이에요. 급여량과 활동량을 조금씩 조절해 보세요."
            else ->
                "${pet.name}의 기록에서 살펴볼 부분이 있어요. 아래 내용을 확인해 주세요."
        }

        val insights = insights(pet, sorted, meals, stage, range, today)

        return HealthReport(chips, headline, status, currentWeight, range, feeding, alerts, insights)
    }

    private fun vitalAlerts(pet: Pet, latest: HealthRecord?, today: LocalDate): List<Alert> {
        latest ?: return emptyList()
        if (ChronoUnit.DAYS.between(latest.date, today) > 3) return emptyList()
        val list = mutableListOf<Alert>()
        val tr = temperatureRange(pet.species)
        latest.temperatureC?.let { t ->
            when {
                t >= 40.0 -> list += Alert(Severity.WARNING, "체온이 높아요 (${fmt(t)}°C)", "정상 범위(${fmt(tr.min)}~${fmt(tr.max)}°C)보다 높아요. 발열이 의심되니 다시 측정해 보고 계속 높으면 동물병원에 문의하세요.")
                t > tr.max -> list += Alert(Severity.CAUTION, "체온이 조금 높아요 (${fmt(t)}°C)", "흥분하거나 운동한 직후라면 30분 쉬고 다시 측정해 보세요.")
                t < 37.0 -> list += Alert(Severity.WARNING, "체온이 낮아요 (${fmt(t)}°C)", "저체온이 의심돼요. 따뜻하게 해 주고 동물병원에 문의하세요.")
                t < tr.min -> list += Alert(Severity.CAUTION, "체온이 조금 낮아요 (${fmt(t)}°C)", "측정 위치와 방법을 확인하고 다시 측정해 보세요.")
            }
        }
        val hr = heartRateRange(pet.species)
        latest.heartRateBpm?.let { bpm ->
            when {
                bpm > hr.max -> list += Alert(Severity.CAUTION, "심박수가 높아요 (${bpm}bpm)", "안정된 상태에서 15초간 센 뒤 ×4로 다시 측정해 보세요. 참고 범위: ${hr.min.toInt()}~${hr.max.toInt()}bpm")
                bpm < hr.min -> list += Alert(Severity.CAUTION, "심박수가 낮아요 (${bpm}bpm)", "잠든 상태가 아니라면 다시 측정해 보세요. 참고 범위: ${hr.min.toInt()}~${hr.max.toInt()}bpm")
            }
        }
        return list
    }

    /** 최근 30일 안에서 체중이 10% 이상 변했는지 확인 */
    fun weightChangeAlert(weighed: List<HealthRecord>, today: LocalDate): Alert? {
        val recent = weighed.filter { ChronoUnit.DAYS.between(it.date, today) in 0..30 }
        if (recent.size < 2) return null
        val first = recent.first().weightKg ?: return null
        val last = recent.last().weightKg ?: return null
        if (first <= 0) return null
        val change = (last - first) / first
        if (abs(change) < 0.10) return null
        val days = ChronoUnit.DAYS.between(recent.first().date, recent.last().date).coerceAtLeast(1)
        val pct = (abs(change) * 100).roundToInt()
        val dir = if (change > 0) "늘었어요" else "줄었어요"
        return Alert(
            Severity.WARNING,
            "체중이 ${days}일 만에 ${pct}% $dir",
            "${fmt(first)}kg → ${fmt(last)}kg. 입력 실수가 아니라면 급격한 체중 변화는 질환의 신호일 수 있어 병원 상담을 권해요.",
        )
    }

    /** 최근 3일 중 2일 이상 같은 증상이 기록되면 경고, 1일이면 관찰 안내 */
    fun symptomAlerts(records: List<HealthRecord>, today: LocalDate): List<Alert> {
        val recent = records.filter { ChronoUnit.DAYS.between(it.date, today) in 0..2 }
        val counts = recent.flatMap { r -> r.tags.filter { it in SYMPTOM_TAGS }.distinct() }.groupingBy { it }.eachCount()
        return counts.entries.sortedByDescending { it.value }.map { (tag, n) ->
            if (n >= 2) {
                Alert(Severity.WARNING, "'$tag' 증상이 반복돼요", "최근 3일 중 ${n}일 기록됐어요. 증상이 이어지면 동물병원 진료를 권해요.")
            } else {
                Alert(Severity.INFO, "'$tag' 기록이 있어요", "내일도 같은 증상이 있는지 관찰하고 기록해 주세요.")
            }
        }
    }

    private fun insights(
        pet: Pet,
        records: List<HealthRecord>,
        meals: List<MealLog>,
        stage: LifeStage,
        range: WeightRange?,
        today: LocalDate,
    ): List<String> = buildList {
        val weekWeights = records.count { it.weightKg != null && ChronoUnit.DAYS.between(it.date, today) in 0..13 }
        if (weekWeights < 2) add("주 1회 체중을 기록해 추세를 확인하면 이상 신호를 더 빨리 알아챌 수 있어요.")
        val mealDays = meals.filter { ChronoUnit.DAYS.between(it.date, today) in 0..6 }.map { it.date }.distinct().size
        if (mealDays in 1..4) add("최근 7일 중 ${mealDays}일만 먹이 기록이 있어요. 매일 기록하면 식욕 변화를 놓치지 않아요.")
        if (records.none { it.date == today }) add("저녁에 오늘의 체중·체온·심박수를 기록해 주세요. 먹이 정보를 먼저 입력하면 기록할 수 있어요.")
        if (range != null && !range.basedOnBreed) add("품종을 입력하면 품종별 권장 체중 범위로 더 정확하게 알려드려요.")
        when (stage) {
            LifeStage.BABY -> add("어린 시기에는 예방접종 일정과 체중 증가를 꾸준히 확인해 주세요.")
            LifeStage.SENIOR -> add("노령기에는 6개월마다 건강검진을 받는 것이 좋아요.")
            LifeStage.ADULT -> add("1년에 한 번 정기 건강검진과 예방접종을 챙겨 주세요.")
        }
        if (pet.allergies.isNotBlank()) add("알레르기(${pet.allergies})가 있으니 사료·간식 성분표를 꼭 확인하세요.")
        if (pet.conditions.isNotBlank()) add("기저질환(${pet.conditions})이 있으면 급여량과 사료는 담당 수의사와 상의해 주세요.")
        if (pet.species == Species.DOG) add("하루 2회, 20~30분 산책은 체중 관리와 스트레스 해소에 도움이 돼요.")
        else add("고양이는 물을 적게 마시기 쉬워요. 물그릇을 여러 곳에 두거나 습식 사료를 섞어 보세요.")
    }

    fun round1(v: Double) = (v * 10).roundToInt() / 10.0

    fun fmt(v: Double): String = String.format(Locale.US, "%.1f", v)
}
