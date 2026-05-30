package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.model.Hpp
import com.owl.minerva.stocklab.repository.HppRepository
import kotlinx.coroutines.flow.Flow

class HppService(
    private val hppRepository: HppRepository
) {
    fun index(): Flow<List<Hpp>> = hppRepository.getAll()

    suspend fun show(id: Long): Hpp? = hppRepository.getById(id)

    suspend fun store(hpp: Hpp): Long {
        require(hpp.itemId > 0) { "Item id is required." }
        require(hpp.total >= 0) { "HPP total cannot be negative." }
        require(hpp.amount > 0) { "HPP amount must be greater than zero." }
        return hppRepository.insert(hpp)
    }

    suspend fun update(hpp: Hpp) {
        require(hpp.id > 0) { "HPP id is required for update." }
        require(hpp.itemId > 0) { "Item id is required." }
        require(hpp.total >= 0) { "HPP total cannot be negative." }
        require(hpp.amount > 0) { "HPP amount must be greater than zero." }
        hppRepository.update(hpp.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(hpp: Hpp) = hppRepository.delete(hpp)
}
