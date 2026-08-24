package com.resonote.feature.library.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.UserProfile

@Composable
internal fun AccountCard(
    profile: UserProfile,
    onDailyVipClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 34.dp).testTag("my-profile"),
        verticalArrangement = Arrangement.spacedBy(37.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(96.dp).padding(end = 32.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Avatar(profile, Modifier.offset(y = 6.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp, top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.nickname.ifBlank {
                                stringResource(R.string.feature_library_impl_my_unnamed_user)
                            },
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (profile.isVip) VipLabel(profile.vipLabel)
                    }
                    Row(
                        modifier = Modifier.padding(top = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.feature_library_impl_my_user_id, profile.userId),
                            modifier = Modifier.weight(1f, fill = false).testTag("my-user-id"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        DailyVipAction(onDailyVipClick)
                    }
                    Text(
                        text = profile.signature.takeIf(String::isNotBlank)
                            ?: stringResource(R.string.feature_library_impl_my_default_signature),
                        modifier = Modifier.padding(top = 7.dp).testTag("my-signature"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(96.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                SettingsButton(onSettingsClick, Modifier.offset(y = 9.dp))
            }
        }
        Row(
            Modifier
                .bleedHorizontally(20.dp)
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileStat(
                stringResource(R.string.feature_library_impl_my_follows),
                profile.follows.compactNumber(),
                Modifier.weight(1f).testTag("my-stat-follows"),
                onClick = onFollowingClick,
            )
            ProfileStatDivider()
            ProfileStat(
                stringResource(R.string.feature_library_impl_my_fans),
                profile.fans.compactNumber(),
                Modifier.weight(1f).testTag("my-stat-fans"),
            )
            ProfileStatDivider()
            ProfileStat(
                stringResource(R.string.feature_library_impl_my_listen_time),
                profile.listenMinutes.listenTime(),
                Modifier.weight(1f).testTag("my-stat-listen-time"),
            )
            ProfileStatDivider()
            ProfileStat(
                stringResource(R.string.feature_library_impl_my_music_age),
                profile.musicAgeYears?.let {
                    stringResource(R.string.feature_library_impl_my_music_age_years, it)
                } ?: "—",
                Modifier.weight(1f).testTag("my-stat-music-age"),
            )
        }
    }
}

@Composable
internal fun AnonymousAccountCard(onLoginClick: () -> Unit, onSettingsClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 34.dp).testTag("my-anonymous")) {
        Row(
            modifier = Modifier.fillMaxWidth().height(102.dp).padding(end = 32.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLoginClick,
                    ),
                shape = MaterialTheme.shapes.large,
                color = Color.Transparent,
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        modifier = Modifier
                            .offset(y = 6.dp)
                            .size(96.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                CircleShape,
                            )
                            .padding(3.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, null, Modifier.size(40.dp))
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp, top = 16.dp)) {
                        Text(
                            stringResource(R.string.feature_library_impl_my_anonymous_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.feature_library_impl_my_anonymous_body),
                            Modifier.padding(top = 7.dp),
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            modifier = Modifier.padding(top = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.CardGiftcard,
                                null,
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                stringResource(R.string.feature_library_impl_my_anonymous_vip_note),
                                Modifier.padding(start = 6.dp),
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(96.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            SettingsButton(onSettingsClick, Modifier.offset(y = 9.dp))
        }
    }
}

@Composable
private fun DailyVipAction(onClick: () -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            onClick = onClick,
            modifier = Modifier.padding(start = 8.dp).testTag("my-daily-vip"),
            shape = ResonoteTokens.shapes.full,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(
                text = stringResource(R.string.feature_library_impl_my_daily_vip_check_in),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ResonoteIconButton(
        label = stringResource(R.string.feature_library_impl_settings),
        onClick = onClick,
        modifier = modifier.offset(x = 12.dp).testTag("my-settings"),
    ) {
        Icon(
            Icons.Rounded.Settings,
            null,
            Modifier.testTag("my-settings-icon"),
        )
    }
}

@Composable
private fun VipLabel(label: String) {
    Surface(
        modifier = Modifier.padding(start = 8.dp),
        shape = ResonoteTokens.shapes.full,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            label.ifBlank { stringResource(R.string.feature_library_impl_my_vip) },
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Avatar(profile: UserProfile, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(96.dp)
            .testTag("my-avatar")
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            profile.nickname.take(1).ifBlank { "·" },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        profile.avatarUrl?.let {
            AsyncImage(
                it,
                stringResource(R.string.feature_library_impl_my_avatar, profile.nickname),
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val actionModifier = if (onClick == null) {
        modifier
    } else {
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    }
    Column(actionModifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            label,
            Modifier.padding(top = 2.dp),
            MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ProfileStatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
    )
}

@Composable
internal fun Chevron(modifier: Modifier = Modifier) {
    Icon(
        Icons.Rounded.ChevronRight,
        null,
        modifier,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
