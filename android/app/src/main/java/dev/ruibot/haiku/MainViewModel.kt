package dev.ruibot.haiku

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ruibot.haiku.data.HaikuRepository
import dev.ruibot.haiku.data.Syllables
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PoemState(
    val lines: List<String> = listOf("", "", ""),
    val totalCount: Int = 0,
    val countPerLine: List<Int> = lines.map { 0 },
    val syllables: List<List<List<String>>> = lines.map { listOf() }
)

sealed class UiState { // UI state for the "write" screen
    data class Loading(val poemState: PoemState) : UiState()
    data class Content(val poemState: PoemState) : UiState()
    data class Error(val poemState: PoemState, val message: String) : UiState()
}

private fun UiState.poemState(): PoemState =
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
        // TODO debounce this to avoid too many requests
        fetchSyllablesFor(poemState.lines)

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
                set(id, newInput)
            }
        )
    }

    private fun fetchSyllablesFor(input: List<String>) {
        fetchSyllablesJob?.cancel()
        fetchSyllablesJob = viewModelScope.launch {
            val result: Result<Syllables> = repository.getPoem(input)
            when {
                result.isSuccess -> {
                    val syllables = result.getOrNull()
                    val totalCount = syllables?.count ?: 0
                    val countPerLine = syllables?.split?.map { line -> line.flatten().filter { it.isNotEmpty() } }?.map { it.size } ?: emptyList()
                    val split = syllables?.split ?: emptyList()

                    val poemState = _uiState.value.poemState().copy(
                        totalCount = totalCount,
                        countPerLine = countPerLine,
                        syllables = split
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

                    if (exception !is CancellationException) {
                        Log.e("ViewModel", "OTHER EXCEPTION: $exception")
                        _uiState.value = UiState.Error(
                            poemState = poemState,
                            message = result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    } else {
                        Log.e("ViewModel", "CANCELLATION EXCEPTION: $exception")
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
