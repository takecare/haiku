package dev.ruibot.haiku

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ruibot.haiku.data.HaikuRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FETCH_SYLLABLES_DELAY_MS = 1000L

enum class LoadingState {
    Idle,
    Loading,
    Error,
}

data class LineState(
    val text: String = "",
    val state: LoadingState = LoadingState.Idle,
    val syllables: List<List<String>> = emptyList(),
) {
    val syllableCount: Int
        get() =
            if (syllables.isEmpty()) 0
            else syllables.map { it.size }.reduce { acc, i -> acc + i }
}

data class PoemState(
    val totalCount: Int = 0,
    val lines: List<LineState> = listOf(LineState(), LineState(), LineState())

)

sealed class UiState { // UI state for the "write" screen
    data class Loading(val poemState: PoemState) : UiState()
    data class Content(val poemState: PoemState) : UiState()
    data class Error(val poemState: PoemState, val message: String) : UiState()
}

private fun UiState.poemState() =
    when (this) {
        is UiState.Loading -> {
            this.poemState
        }
        is UiState.Content -> {
            this.poemState
        }
        is UiState.Error -> {
            this.poemState
        }
    }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: HaikuRepository
) : ViewModel() {

    // https://proandroiddev.com/livedata-vs-sharedflow-and-stateflow-in-mvvm-and-mvi-architecture-57aad108816d
    // https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda

    private val _uiState = MutableStateFlow<UiState>(UiState.Content(PoemState()))
    val uiState: StateFlow<UiState> = _uiState

    private var fetchSyllablesJob: Job? = null

    //    private val _event: MutableSharedFlow<Event> = MutableSharedFlow<Event>()
    //    val event = _event.asSharedFlow()

    init {
        // TODO get poem loaded from db
        // viewModelScope.launch { ... }
    }

    // we emit a new state independently of the result of fetching the syllables
    // when the result is returned from the server we update the state again
    fun inputChanged(id: Int, newInput: String) {
        // we immediately derive the new state given the new input we just got
        val poemState = deriveNewPoemState(id, newInput)

        // we then fetch the syllables for this new input
        fetchSyllablesFor(poemState)

        when (val state = _uiState.value) {
            is UiState.Content -> {
                _uiState.value = UiState.Content(poemState)
            }
            is UiState.Error -> {
                _uiState.value = UiState.Error(poemState, state.message)
            }
            is UiState.Loading -> {
                _uiState.value = UiState.Loading(poemState)
            }
        }
    }

    private fun deriveNewPoemState(id: Int, newInput: String): PoemState {
        val syllables: PoemState = _uiState.value.poemState()
        val currentInput = syllables.lines[id]
        return syllables.copy(
            lines = syllables.lines.toMutableList().apply {
                set(id, currentInput.copy(text = newInput, state = LoadingState.Loading))
            }
        )
    }

    private fun fetchSyllablesFor(input: PoemState) {
        fetchSyllablesJob?.cancel()
        fetchSyllablesJob = viewModelScope.launch {

            delay(FETCH_SYLLABLES_DELAY_MS)

            val result = repository.getPoem(input.lines.map { it.text })
            when {
                result.isSuccess -> {
                    val syllables = result.getOrNull()
                    val totalCount = syllables?.count ?: 0
                    val split = syllables?.split ?: emptyList()
                    val currentLines = _uiState.value.poemState().lines

                    val poemState = _uiState.value.poemState().copy(
                        totalCount = totalCount,
                        lines = split.mapIndexed { i, lineSplit ->
                            LineState(
                                text = currentLines[i].text,
                                syllables = lineSplit,
                                state = LoadingState.Idle,
                            )
                        }
                    )

                    _uiState.value = when (_uiState.value) {
                        is UiState.Loading -> UiState.Content(poemState)
                        is UiState.Content -> UiState.Content(poemState)
                        is UiState.Error -> UiState.Content(poemState)
                    }
                }
                result.isFailure -> {
                    val exception = result.exceptionOrNull()
                    val poemState = _uiState.value.poemState()

                    // TODO how to map error to specific line?

                    if (exception !is CancellationException) {
                        Log.e("ViewModel", "OTHER EXCEPTION: $exception")
                        _uiState.value = UiState.Error(
                            poemState = poemState,
                            message = result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    } else {
                        // Log.e("ViewModel", "CANCELLATION EXCEPTION: $exception")
                    }
                }
            }
        }
    }

    // debug only
    fun reset() {
        _uiState.value = UiState.Content(PoemState())
    }
}
