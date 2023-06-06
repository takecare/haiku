package dev.ruibot.haiku.presentation

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject

sealed class MainEvent {
    object NavigateToWrite : MainEvent()
    object NavigateToRead : MainEvent()
    object NavigateToSettings : MainEvent()
}

@Stable
sealed class MainUiState {
    data class Loading(val navItems: List<NavItem> = emptyList()) : MainUiState()
    data class Content(val navItems: List<NavItem>) : MainUiState()
    data class Error(
        val navItems: List<NavItem>,
        val message: String
    ) : MainUiState()
}

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(
        MainUiState.Content(
            navItems = listOf()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState

    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events: Flow<MainEvent> = _events
        .receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 0)


    fun navItemClicked() {
        //
    }

}
