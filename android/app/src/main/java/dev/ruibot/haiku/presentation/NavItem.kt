package dev.ruibot.haiku.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable

// sealed interface _NavItem {
//     val x: String
//
//     data class Write(override val x: String = "") : _NavItem
//
//     data class Read(override val x: String = "") : _NavItem
//     object Settings : _NavItem
// }

sealed class NavItem {
    @get:StringRes
    abstract val title: Int

    @get:StringRes
    abstract val destination: Int

    data class Write(
        override val title: Int = 0,
        override val destination: Int = 0
    ) : NavItem()

    data class Read(
        override val title: Int = 0,
        override val destination: Int = 0
    ) : NavItem()

    data class Settings(
        override val title: Int = 0,
        override val destination: Int = 0
    ) : NavItem()
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
