package dev.ruibot.haiku.presentation.write

sealed class WriteUiState { // UI state for the "write" screen
    data class Loading(val poemState: PoemState) : WriteUiState() // FIXME not really used
    data class Content(val poemState: PoemState) : WriteUiState()
    data class Error(
        val poemState: PoemState,
        val message: String
    ) : WriteUiState()
}

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
    /**
     * Returns the syllable count for this line by looking at [syllables].
     */
    val syllableCount: Int
        get() =
            if (syllables.isEmpty()) 0
            else syllables.map { it.size }.reduce { acc, i -> acc + i }
}

data class PoemState(
    val totalCount: Int = 0,
    val lines: List<LineState> = listOf(LineState(), LineState(), LineState())
) {
    val loadingState: LoadingState
        get() = lines.map { it.state }.run {
            firstOrNull { it == LoadingState.Loading }
                ?: firstOrNull { it == LoadingState.Error }
                ?: LoadingState.Idle
        }
}
