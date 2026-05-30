package com.owl.minerva.stocklab.service

import kotlin.random.Random

object RecordCodeGenerator {
    fun itemCode(name: String): String {
        val normalized = name.uppercase().filter { it.isLetterOrDigit() }
        val consonants = normalized.filter { it.isLetter() && it !in VOWELS }
        val vowels = normalized.filter { it in VOWELS }
        val slug = (consonants + vowels)
            .take(5)
            .ifBlank { "ITEM" }
        val suffix = Random.nextInt(1000, 10000)

        return "$slug$suffix"
    }

    fun batchCode(itemCode: String, sequence: Int): String = recordCode(itemCode, "B", sequence)

    fun ledgerCode(itemCode: String, sequence: Int): String = recordCode(itemCode, "L", sequence)

    fun stockInCode(itemCode: String, sequence: Int): String = recordCode(itemCode, "SI", sequence)

    fun stockOutCode(itemCode: String, sequence: Int): String = recordCode(itemCode, "SO", sequence)

    private fun recordCode(itemCode: String, segment: String, sequence: Int): String {
        return "$itemCode/$segment/$sequence"
    }

    private val VOWELS = setOf('A', 'I', 'U', 'E', 'O')
}
