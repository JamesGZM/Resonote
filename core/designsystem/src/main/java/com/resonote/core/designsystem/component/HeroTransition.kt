@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.resonote.core.designsystem.component

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.navigation3.ui.LocalNavAnimatedContentScope

private val LocalResonoteSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

@Composable
fun ResonoteHeroTransitionLayout(content: @Composable () -> Unit) {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalResonoteSharedTransitionScope provides this) {
            content()
        }
    }
}

@Composable
fun Modifier.resonoteHero(key: String?): Modifier {
    if (key.isNullOrBlank()) return this
    val sharedTransitionScope = LocalResonoteSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalNavAnimatedContentScope.current
    return with(sharedTransitionScope) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animatedVisibilityScope,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Crop),
        )
    }
}

object ResonoteHeroKeys {
    fun playlist(id: String): String = "playlist:${id.trim()}"
    fun ranking(id: String): String = "ranking:${id.trim()}"
    fun album(id: String): String = "album:${id.trim()}"
    fun artist(id: String): String = "artist:${id.trim()}"
    fun video(hash: String): String = "video:${hash.trim()}"
}
