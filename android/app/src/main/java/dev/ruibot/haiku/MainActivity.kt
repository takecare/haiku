package dev.ruibot.haiku

import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import dev.ruibot.haiku.ui.theme.HaikuTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.lang.Exception
import java.net.ConnectException
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun httpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

    @Provides
    fun provideOkHttpClient(httpLoggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .build()

    @Provides
    fun provideHaikuService(okHttpClient: OkHttpClient): HaikuService =
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create()
}

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

data class UiSyllables( // TODO better name
    val totalCount: Int = 0,
    val countPerLine: List<Int> = emptyList(),
    val syllables: List<List<String>> = emptyList()
)

sealed class WriteUiState {
    object Loading : WriteUiState()
    data class Content(val syllables: UiSyllables) : WriteUiState()
    data class Error(val message: String) : WriteUiState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: HaikuRepository
) : ViewModel() {

    // https://proandroiddev.com/livedata-vs-sharedflow-and-stateflow-in-mvvm-and-mvi-architecture-57aad108816d
    // https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda

    private val _uiState = MutableStateFlow<WriteUiState>(WriteUiState.Content(UiSyllables()))
    val uiState: StateFlow<WriteUiState> = _uiState

//    private val _event: MutableSharedFlow<Event> = MutableSharedFlow<Event>()
//    val event = _event.asSharedFlow()

    init {
        _uiState.value = WriteUiState.Loading
        // TODO get poem loaded from db
//        viewModelScope.launch {
//            repository.getPoem(listOf("palavra"))
//        }
    }

    fun load(input: List<String>) {
        viewModelScope.launch {
            val result: Result<Syllables> = repository.getPoem(input)
            when {
                result.isSuccess -> {
                    Log.d("ViewModel", result.toString())
                    val totalCount = result.getOrNull()?.count ?: 0
                    val countPerLine = result.getOrNull()?.split?.map { it.size } ?: emptyList()
                    val split = result.getOrNull()?.split ?: emptyList()
                    _uiState.value = WriteUiState.Content(
                        UiSyllables(totalCount, countPerLine, split)
                    )
                }
                result.isFailure -> {
                    // TODO emit error
                    Log.e("ViewModel", result.toString())
                }
            }
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(viewModel = viewModel())
        }
    }
}

@Composable
fun Screen(content: @Composable() () -> Unit) {
    HaikuTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            content()
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    Screen {
        // TODO get content from text inputs
        Greeting("Android", onClick = { viewModel.load(listOf("primeira linha", "segunda linha")) })
    }
}

@Composable
fun Greeting(name: String, onClick: () -> Unit) {
    Column {
        Button(onClick = onClick) {
            Text(text = "Hello $name!")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Screen {
        Greeting("Android", onClick = { })
    }
}
