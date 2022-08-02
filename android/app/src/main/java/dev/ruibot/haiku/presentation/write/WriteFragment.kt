package dev.ruibot.haiku.presentation.write

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import dagger.hilt.android.AndroidEntryPoint
import dev.ruibot.haiku.R
import dev.ruibot.haiku.databinding.FragmentWriteBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class WriteFragment : Fragment() {

    private var _binding: FragmentWriteBinding? = null
    private val binding get() = _binding!! // This property is only valid between onCreateView and onDestroyView

    private val viewModel by viewModels<WriteViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWriteBinding.inflate(inflater, container, false)
        val view = binding.root

        // this is the best way i could find to get the nav controller in our situation
        val navController = Navigation.findNavController(container as View)
        // val navigator = navController.navigatorProvider.navigators["fragment"]
        // navController.navigate(R.id.settings_screen)

        binding.composeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WriteScreen(viewModel = viewModel, navController = navController)
            }
            this.compositionContext
        }

        viewModel.events
            .flowWithLifecycle(lifecycle)
            .onEach { Log.d("EVENT", "FRAGMENT: $it") }
            .launchIn(lifecycleScope)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class) // collectAsStateWithLifecycle
@Composable
fun WriteScreen(
    viewModel: WriteViewModel,
    navController: NavController
) {

    // https://medium.com/tech-takeaways/how-to-safely-collect-flows-lifecycle-aware-in-jetpack-compose-a-new-approach-ed20ead25be9
    // https://proandroiddev.com/how-to-collect-flows-lifecycle-aware-in-jetpack-compose-babd53582d0b

    val state: WriteUiState by viewModel.uiState.collectAsStateWithLifecycle(
        initialValue = WriteUiState.Content(PoemState())
    )

    viewModel.events
        .flowWithLifecycle(lifecycle = LocalLifecycleOwner.current.lifecycle)
        .onEach { Log.d("EVENT", "COMPOSE: $it") }
        .launchIn(LocalLifecycleOwner.current.lifecycleScope)

    val lines = when (state) {
        is WriteUiState.Content -> {
            val content = state as WriteUiState.Content
            content.poemState.lines
        }
        is WriteUiState.Error -> {
            val error = state as WriteUiState.Error
            error.poemState.lines
        }
        is WriteUiState.Loading -> {
            val loading = state as WriteUiState.Loading
            loading.poemState.lines
        }
    }

    // val scaffoldState = rememberScaffoldState()
    val snackbarHostState = remember { SnackbarHostState() }

    if (state is WriteUiState.Error) {
        // https://developer.android.com/jetpack/compose/side-effects
        LaunchedEffect(state) {
            val result = snackbarHostState
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

    val _navController = remember { navController }

    Box(modifier = Modifier.padding(16.dp)) {
        Lines(
            lines = lines,
            onValueChange = { id, input -> viewModel.inputChanged(id, input) },
        )
        Button(onClick = {
            viewModel.testeClicked()
            _navController.navigate(R.id.settings_screen)
        }) {
            Text(text = "TESTE")
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            dev.ruibot.haiku.presentation.HaikuSnackbar(
                snackbarHostState = snackbarHostState,
                onAction = { snackbarHostState.currentSnackbarData?.performAction() },
                modifier = Modifier.align(Alignment.BottomCenter)
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
            colors = TextFieldDefaults.textFieldColors(
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
// endregion
