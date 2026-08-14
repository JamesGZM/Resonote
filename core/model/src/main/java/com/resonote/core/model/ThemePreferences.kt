package com.resonote.core.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}

data class ThemePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
)
