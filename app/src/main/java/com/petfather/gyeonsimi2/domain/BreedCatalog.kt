package com.petfather.gyeonsimi2.domain

import com.petfather.gyeonsimi2.data.db.Species

/**
 * 품종별 성체 참고 체중(kg). 일반적으로 알려진 범위를 단순화한 값이며 진단 기준이 아닙니다.
 * 목록에 없는 품종(믹스 포함)은 등록 체중을 기준으로 범위를 계산합니다.
 */
data class BreedInfo(
    val name: String,
    val species: Species,
    val minKg: Double,
    val maxKg: Double,
    val aliases: List<String> = emptyList(),
)

object BreedCatalog {
    val all: List<BreedInfo> = listOf(
        // 강아지
        BreedInfo("말티즈", Species.DOG, 2.0, 4.0, listOf("말티스")),
        BreedInfo("토이 푸들", Species.DOG, 2.0, 4.0, listOf("푸들", "토이푸들")),
        BreedInfo("미니어처 푸들", Species.DOG, 4.5, 8.0, listOf("미니푸들")),
        BreedInfo("포메라니안", Species.DOG, 1.8, 3.5, listOf("포메")),
        BreedInfo("치와와", Species.DOG, 1.5, 3.0),
        BreedInfo("요크셔테리어", Species.DOG, 2.0, 3.5, listOf("요크셔", "요키")),
        BreedInfo("시츄", Species.DOG, 4.0, 7.5, listOf("시추")),
        BreedInfo("비숑 프리제", Species.DOG, 5.0, 8.0, listOf("비숑")),
        BreedInfo("미니어처 슈나우저", Species.DOG, 5.0, 9.0, listOf("슈나우저")),
        BreedInfo("닥스훈트", Species.DOG, 7.0, 14.5, listOf("닥스")),
        BreedInfo("프렌치 불독", Species.DOG, 8.0, 13.0, listOf("프렌치불독", "프불")),
        BreedInfo("시바견", Species.DOG, 7.0, 11.0, listOf("시바", "시바이누")),
        BreedInfo("비글", Species.DOG, 9.0, 14.0),
        BreedInfo("웰시코기", Species.DOG, 10.0, 14.0, listOf("코기", "웰시 코기")),
        BreedInfo("보더콜리", Species.DOG, 12.0, 20.0, listOf("보더 콜리")),
        BreedInfo("진돗개", Species.DOG, 15.0, 23.0, listOf("진도", "진도개")),
        BreedInfo("사모예드", Species.DOG, 16.0, 30.0),
        BreedInfo("시베리안 허스키", Species.DOG, 16.0, 27.0, listOf("허스키")),
        BreedInfo("골든 리트리버", Species.DOG, 25.0, 34.0, listOf("골든리트리버", "골든")),
        BreedInfo("래브라도 리트리버", Species.DOG, 25.0, 36.0, listOf("래브라도", "라브라도")),
        // 고양이
        BreedInfo("코리안 숏헤어", Species.CAT, 3.5, 5.5, listOf("코숏", "코리안숏헤어")),
        BreedInfo("페르시안", Species.CAT, 3.0, 5.5, listOf("페르시안 고양이")),
        BreedInfo("러시안 블루", Species.CAT, 3.0, 5.5, listOf("러시안블루", "러블")),
        BreedInfo("브리티시 숏헤어", Species.CAT, 4.0, 8.0, listOf("브숏", "브리티쉬 숏헤어")),
        BreedInfo("스코티시 폴드", Species.CAT, 3.0, 6.0, listOf("스코티쉬 폴드", "스코티시폴드")),
        BreedInfo("아메리칸 숏헤어", Species.CAT, 3.5, 7.0, listOf("아메숏", "아메리칸숏헤어")),
        BreedInfo("먼치킨", Species.CAT, 2.5, 4.5),
        BreedInfo("랙돌", Species.CAT, 4.5, 9.0),
        BreedInfo("노르웨이 숲", Species.CAT, 4.0, 9.0, listOf("노르웨이숲", "노숲")),
        BreedInfo("샴", Species.CAT, 3.0, 5.0, listOf("샴고양이")),
        BreedInfo("벵갈", Species.CAT, 3.5, 7.0),
        BreedInfo("메인쿤", Species.CAT, 5.0, 11.0, listOf("메인 쿤")),
    )

    fun namesFor(species: Species): List<String> = all.filter { it.species == species }.map { it.name }

    fun find(species: Species, breed: String): BreedInfo? {
        val key = breed.normalized()
        if (key.isEmpty()) return null
        return all.firstOrNull { info ->
            info.species == species && (info.name.normalized() == key || info.aliases.any { it.normalized() == key })
        }
    }

    private fun String.normalized() = replace(" ", "").lowercase()
}
