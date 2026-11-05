package com.petfather.gyeonsimi.data.repo

import com.petfather.gyeonsimi.data.db.Pet
import com.petfather.gyeonsimi.data.db.PetDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PetRepository(
    private val dao: PetDao,
    private val settings: SettingsRepository,
) {
    val pets: Flow<List<Pet>> = dao.observeAll()

    /** 선택한 반려동물. 선택값이 없거나 삭제됐다면 처음 등록한 반려동물을 씁니다. */
    val activePet: Flow<Pet?> = combine(pets, settings.settings) { list, s ->
        list.firstOrNull { it.id == s.selectedPetId } ?: list.firstOrNull()
    }

    suspend fun get(id: Long): Pet? = dao.get(id)

    suspend fun add(pet: Pet): Long {
        val id = dao.insert(pet.copy(id = 0))
        settings.selectPet(id)
        return id
    }

    suspend fun update(pet: Pet) = dao.update(pet)

    suspend fun delete(pet: Pet) = dao.delete(pet)

    suspend fun select(id: Long) = settings.selectPet(id)
}
