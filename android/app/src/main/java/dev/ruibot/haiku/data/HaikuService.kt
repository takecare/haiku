package dev.ruibot.haiku.data

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class Word(
    @field:Json(name = "count") val count: Int,
    @field:Json(name = "split") val split: List<String>
)

data class Syllables(
    @field:Json(name = "count") val count: Int,
    @field:Json(name = "split") val split: List<List<List<String>>>
)

data class Poem(
    @field:Json(name = "body") val body: List<String>
)

interface HaikuService {
    @GET("word/{word}")
    suspend fun word(@Path("word") word: String): Word

    @GET("line/{line}")
    suspend fun line(@Path("line") word: String): List<Word>

    @POST("poem")
    suspend fun poem(@Body poem: Poem): Syllables
}
