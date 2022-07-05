package dev.ruibot.haiku.data

import java.lang.Exception
import javax.inject.Inject

class HaikuRepository @Inject constructor(
    private val service: HaikuService
) {
    // TODO kotlin flows
    // TODO we probably only need getPoem()
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

    suspend fun getPoem(poem: List<String>): Result<Syllables> {
        return try {
            val words = service.poem(Poem(poem))
            return Result.success(words)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
