package dev.ruibot.haiku.data

import dev.ruibot.haiku.domain.SyllablesRepository
import javax.inject.Inject
import dev.ruibot.haiku.domain.Syllables as DomainSyllables

class HaikuRepository @Inject constructor(
    private val service: HaikuService
) : SyllablesRepository {
    // TODO database: expose flow and method to fetch data (that also writes to db, updating the flow)

    // https://developer.android.com/kotlin/flow
    // https://developer.android.com/kotlin/flow/stateflow-and-sharedflow

    suspend fun getWord(word: String): Result<Word> {
        return try {
            return Result.success(service.word(word))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getLine(words: String): Result<List<Word>> {
        return try {
            return Result.success(service.line(words))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Issues a network request to get the total syllable count
     * This is a main-safe suspend function as we use Retrofit under the hood,
     * which handles switching to an appropriate thread.
     */
    override suspend fun getPoem(lines: List<String>): Result<DomainSyllables> {
        // no need to switch coroutine context as retrofit handles that (as it
        // sees the function in our retrofit service marked with "suspend")
        return try {
            val poem = Poem(lines.map { it.trim() })
            val syllables = service.poem(poem)
            val domain = DomainSyllables(
                totalCount = syllables.count,
                syllables = syllables.split
            )
            return Result.success(domain)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
