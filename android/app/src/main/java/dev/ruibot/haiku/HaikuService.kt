package dev.ruibot.haiku

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path

data class Word(
    @field:Json(name = "count") val count: Int,
    @field:Json(name = "split") val split: List<String>
)

interface HaikuService {
    @GET("word/{word}")
    suspend fun word(@Path("word") word: String): Word
}
