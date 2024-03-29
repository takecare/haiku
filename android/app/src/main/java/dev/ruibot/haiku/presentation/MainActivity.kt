package dev.ruibot.haiku.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults.textFieldColors
import androidx.compose.material.TopAppBar
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.ruibot.haiku.presentation.theme.HaikuTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(viewModel = viewModel())
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class) // collectAsStateWithLifecycle
@Composable
fun MainScreen(viewModel: MainViewModel) {

    // https://medium.com/tech-takeaways/how-to-safely-collect-flows-lifecycle-aware-in-jetpack-compose-a-new-approach-ed20ead25be9
    // https://proandroiddev.com/how-to-collect-flows-lifecycle-aware-in-jetpack-compose-babd53582d0b

    val state: UiState by viewModel.uiState.collectAsStateWithLifecycle(
        initialValue = UiState.Content(PoemState())
    )

    val lines = when (state) {
        is UiState.Content -> {
            val content = state as UiState.Content
            content.poemState.lines
        }
        is UiState.Error -> {
            val error = state as UiState.Error
            error.poemState.lines
        }
        is UiState.Loading -> {
            val loading = state as UiState.Loading
            loading.poemState.lines
        }
    }

    val scaffoldState = rememberScaffoldState()

    if (state is UiState.Error) {
        // https://developer.android.com/jetpack/compose/side-effects
        LaunchedEffect(state) {
            val result = scaffoldState.snackbarHostState
                .showSnackbar(
                    message = "Failed to load syllable information.", // TODO stringResource(id=...)
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Indefinite
                )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    viewModel.retry()
                }
                SnackbarResult.Dismissed -> {
                    // no op
                }
            }
        }
    }

    Screen(
        scaffoldState = scaffoldState,
        title = "Haiku: Compose", // TODO stringResource(id=...)
        onActionClicked = { viewModel.reset() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Lines(
                lines = lines,
                onValueChange = { id, input -> viewModel.inputChanged(id, input) },
            )
        }
    }
}

@Composable
fun Lines(
    modifier: Modifier = Modifier,
    lines: List<LineState> = emptyList(),
    onValueChange: (Int, String) -> Unit = { _, _ -> }
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        lines.forEachIndexed { index, line ->
            Line(
                modifier = modifier,
                line = line,
                onValueChange = { onValueChange(index, it) }
            )
        }
        // TODO button to add one more line
    }
}

@Composable
fun Line(
    modifier: Modifier = Modifier,
    line: LineState,
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
            value = line.text,
            onValueChange = onValueChange
        )
        Row(
            modifier = modifier.fillMaxWidth(fraction = 1f),
            horizontalArrangement = Arrangement.Center
        ) {
            SyllableCount(count = line.syllableCount, state = line.state)
        }
    }
}

@Composable
fun SyllableCount(count: Int = 0, state: LoadingState) {
    val infiniteTransition = rememberInfiniteTransition()
    val loadingColor by infiniteTransition.animateColor(
        initialValue = Color.Unspecified,
        targetValue = Color.Gray,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
            },
            repeatMode = RepeatMode.Reverse
        )
    )

    Text(
        text = "$count",
        color = when (state) {
            LoadingState.Loading -> loadingColor
            LoadingState.Idle -> Color.Unspecified
            LoadingState.Error -> Color.Red
        }
    )
}

@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    title: String = "Haiku",
    onActionClicked: () -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(title) },
        actions = {
            IconButton(onClick = onActionClicked) {
                Icon(Icons.Filled.Delete, contentDescription = "Localized action description")
            }
        }
    )
}

@Composable
fun Screen(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    title: String,
    onActionClicked: () -> Unit = {},
    content: @Composable() () -> Unit
) {
    HaikuTheme {
        Scaffold(
            scaffoldState = scaffoldState,
            snackbarHost = {
                // we provide no SnackbarHost as we have our own in our custom
                // snackbar composable. if we use the default one from the
                // Scaffold we either can't customise it or we end up showing
                // both ours and the default one
            },
            topBar = {
                AppBar(
                    title = title,
                    onActionClicked = onActionClicked,
                )
            },
        ) { contentPadding ->
            Column(modifier = modifier.padding(contentPadding)) {
                content()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            HaikuSnackbar(
                snackbarHostState = scaffoldState.snackbarHostState,
                onAction = { scaffoldState.snackbarHostState.currentSnackbarData?.performAction() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun HaikuSnackbar(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onAction: () -> Unit = {},
) {
    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data: SnackbarData ->
            Snackbar(
                content = {
                    Text(
                        text = data.message,
                        style = MaterialTheme.typography.body2
                    )
                },
                action = {
                    data.actionLabel?.let { actionLabel ->
                        TextButton(onClick = onAction) {
                            Text(
                                text = actionLabel,
                                color = MaterialTheme.colors.primary,
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }
                },
                backgroundColor = MaterialTheme.colors.background,
                contentColor = contentColorFor(MaterialTheme.colors.background)
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Bottom)
    )
}

// region COMPOSE PREVIEWS
@Preview(showBackground = true)
@Composable
fun LinesPreview() {
    Lines(
        lines = listOf(
            LineState(
                text = "primeira linha",
                state = LoadingState.Loading,
                syllables = listOf(listOf("pri", "mei", "ra"), listOf("li", "nha"))
            ),
            LineState(
                text = "outra linha",
                state = LoadingState.Idle,
                syllables = listOf(listOf("out", "tra"), listOf("li", "nha"))
            ),
        )
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Screen(title = "Haiku: Compose") {
        Lines(
            lines = listOf(
                LineState(
                    text = "primeira linha",
                    state = LoadingState.Loading,
                    syllables = listOf(listOf("pri", "mei", "ra"), listOf("li", "nha"))
                ),
                LineState(
                    text = "outra linha",
                    state = LoadingState.Idle,
                    syllables = listOf(listOf("out", "tra"), listOf("li", "nha"))
                ),
            )
        )
    }
}
// endregion
