package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.model.HppComponent
import com.owl.minerva.stocklab.repository.HppComponentRepository
import kotlinx.coroutines.flow.Flow

class HppComponentService(
    private val hppComponentRepository: HppComponentRepository
) {
    fun index(): Flow<List<HppComponent>> = hppComponentRepository.getAll()

    suspend fun show(id: Long): HppComponent? = hppComponentRepository.getById(id)

    suspend fun store(hppComponent: HppComponent): Long {
        require(hppComponent.hppId > 0) { "HPP id is required." }
        require(hppComponent.name.isNotBlank()) { "HPP component name cannot be blank." }
        require(hppComponent.amount >= 0) { "HPP component amount cannot be negative." }
        return hppComponentRepository.insert(hppComponent)
    }

    suspend fun update(hppComponent: HppComponent) {
        require(hppComponent.id > 0) { "HPP component id is required for update." }
        require(hppComponent.hppId > 0) { "HPP id is required." }
        require(hppComponent.name.isNotBlank()) { "HPP component name cannot be blank." }
        require(hppComponent.amount >= 0) { "HPP component amount cannot be negative." }
        hppComponentRepository.update(hppComponent.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(hppComponent: HppComponent) = hppComponentRepository.delete(hppComponent)
}
