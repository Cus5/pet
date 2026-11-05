package com.petfather.gyeonsimi.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petfather.gyeonsimi.AppContainer
import com.petfather.gyeonsimi.data.db.Pet
import com.petfather.gyeonsimi.data.db.Sex
import com.petfather.gyeonsimi.data.db.Species
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PetForm(
    val species: Species = Species.DOG,
    val name: String = "",
    val breed: String = "",
    val birthDate: LocalDate? = null,
    val weight: String = "",
    val sex: Sex? = null,
    val neutered: Boolean = false,
    val allergies: String = "",
    val conditions: String = "",
    val editingId: Long? = null,
    val showErrors: Boolean = false,
) {
    val weightKg: Double? get() = weight.toDoubleOrNull()
    val nameError: Boolean get() = name.isBlank()
    val sexError: Boolean get() = sex == null
    val weightError: Boolean get() = weight.isNotBlank() && (weightKg == null || weightKg!! !in 0.1..120.0)
    val isValid: Boolean get() = !nameError && !sexError && !weightError
}

class RegisterViewModel(private val c: AppContainer, private val editId: Long?) : ViewModel() {
    private val _form = MutableStateFlow(PetForm())
    val form: StateFlow<PetForm> = _form.asStateFlow()

    init {
        if (editId != null) {
            viewModelScope.launch {
                c.pets.get(editId)?.let { p ->
                    _form.value = PetForm(
                        species = p.species,
                        name = p.name,
                        breed = p.breed,
                        birthDate = p.birthDate,
                        weight = p.weightKg?.toString().orEmpty(),
                        sex = p.sex,
                        neutered = p.neutered,
                        allergies = p.allergies,
                        conditions = p.conditions,
                        editingId = p.id,
                    )
                }
            }
        }
    }

    fun update(transform: (PetForm) -> PetForm) = _form.update(transform)

    fun today(): LocalDate = c.today()

    /** 저장에 성공하면 true */
    suspend fun save(): Boolean {
        val f = _form.value
        if (!f.isValid) {
            _form.update { it.copy(showErrors = true) }
            return false
        }
        val existing = f.editingId?.let { c.pets.get(it) }
        val pet = Pet(
            id = existing?.id ?: 0,
            name = f.name.trim(),
            species = f.species,
            breed = f.breed.trim(),
            birthDate = f.birthDate,
            sex = f.sex!!,
            neutered = f.neutered,
            weightKg = f.weightKg,
            allergies = f.allergies.trim(),
            conditions = f.conditions.trim(),
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        if (existing != null) c.pets.update(pet) else c.pets.add(pet)
        if (existing == null) _form.value = PetForm()
        return true
    }
}
