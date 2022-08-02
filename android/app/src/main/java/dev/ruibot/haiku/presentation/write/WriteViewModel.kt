package dev.ruibot.haiku.presentation.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ruibot.haiku.domain.GetPoemSyllablesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FETCH_SYLLABLES_DELAY_MS = 1200L

sealed class WriteEvent {
    object NavigateToSettings : WriteEvent()
}

@HiltViewModel
class WriteViewModel @Inject constructor(
    private val useCase: GetPoemSyllablesUseCase
) : ViewModel() {

    // https://proandroiddev.com/livedata-vs-sharedflow-and-stateflow-in-mvvm-and-mvi-architecture-57aad108816d
    // https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda
    private val _uiState = MutableStateFlow<WriteUiState>(WriteUiState.Content(PoemState()))
    val uiState: StateFlow<WriteUiState> = _uiState

    // https://medium.com/androiddevelopers/viewmodel-one-off-event-antipatterns-16a1da869b95
    // https://proandroiddev.com/sending-view-model-events-to-the-ui-eef76bdd632c
    private val _events = Channel<WriteEvent>(Channel.BUFFERED)
    val events: Flow<WriteEvent> = _events
        .receiveAsFlow()
        // this is dangerous (allows handling the same value multiple times) but
        // sharing enables both compose and the fragment to observe/collect this
        .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    private var fetchSyllablesJob: Job? = null

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
            is WriteUiState.Content -> {
                _uiState.value = WriteUiState.Loading(poemState)
            }
            is WriteUiState.Error -> {
                _uiState.value = WriteUiState.Error(poemState, state.message)
            }
            is WriteUiState.Loading -> {
                _uiState.value = WriteUiState.Loading(poemState)
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
                        is WriteUiState.Loading -> WriteUiState.Content(poemState)
                        is WriteUiState.Content -> WriteUiState.Content(poemState)
                        is WriteUiState.Error -> WriteUiState.Content(poemState)
                    }
                }
                result.isFailure -> {
                    val exception = result.exceptionOrNull()
                    val poemState = _uiState.value.poemState()

                    if (exception !is CancellationException) {
                        _uiState.value = WriteUiState.Error(
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
        _uiState.value = WriteUiState.Content(PoemState())
    }

    fun testeClicked() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _events.send(WriteEvent.NavigateToSettings)
        }
    }
}

private fun WriteUiState.poemState() =
    when (this) {
        is WriteUiState.Loading -> {
            this.poemState
        }
        is WriteUiState.Content -> {
            this.poemState
        }
        is WriteUiState.Error -> {
            this.poemState
        }
    }
