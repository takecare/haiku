package dev.ruibot.haiku

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults.textFieldColors
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.ruibot.haiku.ui.theme.HaikuTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(viewModel = viewModel())
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {

    // https://medium.com/tech-takeaways/how-to-safely-collect-flows-lifecycle-aware-in-jetpack-compose-a-new-approach-ed20ead25be9
    // https://proandroiddev.com/how-to-collect-flows-lifecycle-aware-in-jetpack-compose-babd53582d0b
    val state: UiState by viewModel.uiState.collectAsStateWithLifecycle(
        initialValue = UiState.Content(PoemState())
    )

    val lines: List<Pair<String, Int>> = when (state) {
        is UiState.Content -> {
            val content = state as UiState.Content
            val syllables = content.poemState
            Log.d("ViewModel", "content ui state: ${content.poemState.lines.zip(syllables.countPerLine)}")
            content.poemState.lines.zip(syllables.countPerLine)
        }
        is UiState.Error -> {
            Log.d("ViewModel", "> error ui state")
            val error = state as UiState.Error
            val syllables = error.poemState
            error.poemState.lines.zip(syllables.countPerLine)
        }
        is UiState.Loading -> {
            Log.d("ViewModel", "> loading ui state")
            val loading = state as UiState.Error
            val syllables = loading.poemState
            loading.poemState.lines.zip(syllables.countPerLine)
        }
    }

    Screen(title = "Haiku: Compose", onActionClicked = { viewModel.reset() }) {
        Column {
            Lines(
                lines = lines,
                onValueChange = { id, input -> viewModel.inputChanged(id, input) },
            )
        }
    }
}

@Composable
fun Screen(
    modifier: Modifier = Modifier,
    title: String,
    onMenuClick: () -> Unit = {},
    onActionClicked: () -> Unit = {},
    content: @Composable() () -> Unit
) {
    HaikuTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            Column() {
                AppBar(
                    title = title,
                    onMenuClick = onMenuClick,
                    onActionClicked = onActionClicked,
                )
                Column(modifier = modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    title: String = "Haiku",
    onMenuClick: () -> Unit = {},
    onActionClicked: () -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = onActionClicked) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized action description")
            }
        }
    )
}

@Composable
fun Lines(
    modifier: Modifier = Modifier,
    lines: List<Pair<String, Int>> = emptyList(), // List<Pair<Text, SyllableCount>>
    onValueChange: (Int, String) -> Unit = { _, _ -> }
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        lines.forEachIndexed { index, pair ->
            Line(modifier, pair.first, pair.second, onValueChange = { onValueChange(index, it) })
        }
        // TODO button to add one more line
    }
}

@Composable
fun Line(
    modifier: Modifier = Modifier,
    text: String = "",
    count: Int = 0,
    onValueChange: (String) -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            modifier = modifier.fillMaxWidth(fraction = 0.9f),
            singleLine = true,
            maxLines = 1,
            visualTransformation = VisualTransformation.None,
            colors = textFieldColors(
                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0f)
            ),
            value = text,
            onValueChange = onValueChange
        )
        Row(
            modifier = modifier.fillMaxWidth(fraction = 1f),
            horizontalArrangement = Arrangement.Center
            //            contentAlignment = Alignment.Center
        ) {
            Text("$count")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LinesPreview() {
    Lines(
        lines = listOf(Pair("linha", 2), Pair("outra linha", 4))
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Screen(title = "Haiku: Compose") {
        Lines(
            lines = listOf(Pair("", 0), Pair("", 0))
        )
    }
}
