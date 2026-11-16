package com.petfather.gyeonsimi2.domain

import com.petfather.gyeonsimi2.data.db.Species

/**
 * '인기 사료 10가지' 목록. 국내에서 많이 찾는 브랜드를 예시로 정리한 참고용 목록이며
 * 광고·제휴가 아닙니다. 실제 서비스 전에 팀에서 내용을 검토해 갱신하세요.
 */
data class FeedItem(
    val brand: String,
    val product: String,
    val species: Species,
    val stages: Set<LifeStage>,
    val keywords: List<String>,
    val weightControl: Boolean = false,
)

object FeedCatalog {
    private val ALL_STAGES = LifeStage.entries.toSet()
    private val ADULT_UP = setOf(LifeStage.ADULT, LifeStage.SENIOR)

    val items: List<FeedItem> = listOf(
        FeedItem("로얄캐닌", "미니 어덜트", Species.DOG, setOf(LifeStage.ADULT), listOf("소형견", "성견")),
        FeedItem("로얄캐닌", "미니 퍼피", Species.DOG, setOf(LifeStage.BABY), listOf("소형견", "퍼피")),
        FeedItem("힐스 사이언스 다이어트", "어덜트 스몰 바이트", Species.DOG, setOf(LifeStage.ADULT), listOf("성견", "작은 알갱이")),
        FeedItem("힐스 사이언스 다이어트", "어덜트 7+", Species.DOG, setOf(LifeStage.SENIOR), listOf("시니어")),
        FeedItem("퓨리나 프로플랜", "체중 관리", Species.DOG, ADULT_UP, listOf("성견", "체중 관리"), weightControl = true),
        FeedItem("ANF", "6Free", Species.DOG, ALL_STAGES, listOf("전 연령", "곡물 제한")),
        FeedItem("나우 프레쉬", "그레인프리 스몰브리드", Species.DOG, ALL_STAGES, listOf("소형견", "그레인프리")),
        FeedItem("오리젠", "오리지널 독", Species.DOG, ALL_STAGES, listOf("고단백", "전 연령")),
        FeedItem("아카나", "스몰 브리드", Species.DOG, ADULT_UP, listOf("소형견", "고단백")),
        FeedItem("내추럴발란스", "L.I.D. 제한 원료", Species.DOG, ADULT_UP, listOf("제한 원료", "알레르기 고려")),
        FeedItem("로얄캐닌", "인도어", Species.CAT, setOf(LifeStage.ADULT), listOf("실내묘", "성묘")),
        FeedItem("로얄캐닌", "키튼", Species.CAT, setOf(LifeStage.BABY), listOf("키튼")),
        FeedItem("힐스 사이언스 다이어트", "어덜트 퍼펙트 웨이트", Species.CAT, ADULT_UP, listOf("성묘", "체중 관리"), weightControl = true),
        FeedItem("힐스 사이언스 다이어트", "시니어 11+", Species.CAT, setOf(LifeStage.SENIOR), listOf("시니어")),
        FeedItem("퓨리나 프로플랜", "스테릴라이즈드", Species.CAT, ADULT_UP, listOf("중성화묘")),
        FeedItem("오리젠", "캣 앤 키튼", Species.CAT, ALL_STAGES, listOf("고단백", "전 연령")),
        FeedItem("아카나", "와일드 프레리 캣", Species.CAT, ALL_STAGES, listOf("고단백", "그레인프리")),
        FeedItem("나우 프레쉬", "그레인프리 어덜트 캣", Species.CAT, ADULT_UP, listOf("그레인프리", "성묘")),
        FeedItem("웰니스 코어", "인도어", Species.CAT, ADULT_UP, listOf("실내묘", "그레인프리")),
        FeedItem("ANF", "캣 6Free", Species.CAT, ALL_STAGES, listOf("전 연령", "곡물 제한")),
    )

    /**
     * 반려동물 종류에 맞는 10가지를 고르고, 생애 단계와 체중 상태에 맞는 제품을 앞으로 정렬합니다.
     */
    fun recommend(species: Species, stage: LifeStage, status: WeightStatus): List<FeedItem> =
        items.filter { it.species == species }
            .sortedByDescending { score(it, stage, status) }
            .take(10)

    fun score(item: FeedItem, stage: LifeStage, status: WeightStatus): Int {
        var s = 0
        if (stage in item.stages) s += 2 else s -= 3
        if (item.stages.size == 1 && stage in item.stages) s += 1
        if (item.weightControl) s += if (status == WeightStatus.OVER) 3 else -2
        return s
    }

    /** '맞춤' 표시: 생애 단계 전용 제품이거나, 과체중일 때의 체중 관리 제품 */
    fun matches(item: FeedItem, stage: LifeStage, status: WeightStatus): Boolean = when {
        stage !in item.stages -> false
        item.weightControl -> status == WeightStatus.OVER
        else -> item.stages.size < LifeStage.entries.size
    }
}
