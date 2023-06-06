package dev.ruibot.haiku.presentation

import androidx.annotation.StringRes

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
