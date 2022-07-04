package dev.ruibot.haiku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.launch
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
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
    suspend fun getWord(word: String): Word {
        return service.word(word)
//        return Word(0, listOf())
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: HaikuRepository
) : ViewModel() {

    init {
        //
    }

    fun doStuff() {
        viewModelScope.launch {
            val word = repository.getWord("palavra")
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //

        //

        setContent {
            MainScreen(viewModel = viewModel())
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    HaikuTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            Greeting("Android", onClick = { viewModel.doStuff() } )
        }
    }
}

@Composable
fun Greeting(name: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "Hello $name!")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    HaikuTheme {
        Greeting("Android") {}
    }
}
