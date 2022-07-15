package dev.ruibot.haiku.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ruibot.haiku.domain.GetPoemSyllablesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FETCH_SYLLABLES_DELAY_MS = 1200L

@HiltViewModel
class MainViewModel @Inject constructor(
    private val useCase: GetPoemSyllablesUseCase
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
        // viewModelScope.launch { }
    }

    // we emit a new state independently of the result of fetching the syllables
    // when the result is returned from the server we update the state again
    fun inputChanged(id: Int, newInput: String) {
        // we immediately derive the new state given the new input we just got
        val poemState = deriveNewPoemState(id, newInput)

        when (val state = _uiState.value) {
            is UiState.Content -> {
                _uiState.value = UiState.Loading(poemState)
            }
            is UiState.Error -> {
                _uiState.value = UiState.Error(poemState, state.message)
            }
            is UiState.Loading -> {
                _uiState.value = UiState.Loading(poemState)
            }
        }

        // we then fetch the syllables for this new input
        fetchSyllablesFor(poemState)
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

    fun retry() {
        val poemState = _uiState.value.poemState()
        fetchSyllablesFor(poemState)
    }

    private fun fetchSyllablesFor(input: PoemState) {
        fetchSyllablesJob?.cancel()
        fetchSyllablesJob = viewModelScope.launch {

            delay(FETCH_SYLLABLES_DELAY_MS)

            val result = useCase.execute(input.lines.map { it.text })
            when {
                result.isSuccess -> {
                    val syllables = result.getOrNull()
                    val totalCount = syllables?.totalCount ?: 0
                    val split = syllables?.syllables ?: emptyList()
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

                    if (exception !is CancellationException) {
                        _uiState.value = UiState.Error(
                            poemState = poemState.copy(
                                lines = poemState.lines.map { it.copy(state = LoadingState.Error) }
                            ),
                            // we're appending the current time because the launched effect
                            // won't re-trigger the error even if it is a new one, because
                            // the UiState.Error (used as key) looks exactly the same
                            message = (result.exceptionOrNull()?.message ?: "Unknown error") + "${System.currentTimeMillis()}"
                        )
                    } else {
                        // we ignore CancellationExceptions
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
