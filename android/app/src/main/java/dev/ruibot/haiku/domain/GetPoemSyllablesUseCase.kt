package dev.ruibot.haiku.domain

import javax.inject.Inject

data class Syllables(
    val totalCount: Int,
    val syllables: List<List<List<String>>>
)

interface SyllablesRepository {
    suspend fun getPoem(lines: List<String>): Result<Syllables>
}

class GetPoemSyllablesUseCase @Inject constructor(
    private val repository: SyllablesRepository
) {
    suspend fun execute(lines: List<String>): Result<Syllables> {
        return repository.getPoem(lines)
    }
}
