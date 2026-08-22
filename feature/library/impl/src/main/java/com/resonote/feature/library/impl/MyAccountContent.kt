package com.resonote.feature.library.impl

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.UserProfile

@Composable
internal fun AccountCard(profile: UserProfile, onDailyVipClick: () -> Unit, onSettingsClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("my-profile"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile)
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.nickname.ifBlank {
                            stringResource(R.string.feature_library_impl_my_unnamed_user)
                        },
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (profile.isVip) VipLabel(profile.vipLabel)
                }
                Text(
                    stringResource(R.string.feature_library_impl_my_user_id, profile.userId),
                    Modifier.padding(top = 3.dp),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                profile.signature.takeIf(String::isNotBlank)?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            SettingsButton(onSettingsClick)
        }
        DailyVipAction(onDailyVipClick)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ProfileStat(stringResource(R.string.feature_library_impl_my_follows), profile.follows.compactNumber())
            ProfileStat(stringResource(R.string.feature_library_impl_my_fans), profile.fans.compactNumber())
            ProfileStat(
                stringResource(R.string.feature_library_impl_my_listen_time),
                profile.listenMinutes.listenTime(),
            )
        }
    }
}

@Composable
internal fun AnonymousAccountCard(onLoginClick: () -> Unit, onSettingsClick: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onLoginClick,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                color = Color.Transparent,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, null, Modifier.size(30.dp))
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(
                            stringResource(R.string.feature_library_impl_my_anonymous_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.feature_library_impl_my_anonymous_body),
                            Modifier.padding(top = 5.dp),
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                        )
                    }
                }
            }
            SettingsButton(onSettingsClick)
        }
        Row(
            modifier = Modifier.padding(start = 80.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.CardGiftcard,
                null,
                Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.feature_library_impl_my_anonymous_vip_note),
                Modifier.padding(start = 7.dp),
                MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DailyVipAction(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("my-daily-vip"),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp).testTag("my-daily-vip-icon"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CardGiftcard, null, Modifier.size(19.dp))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    stringResource(R.string.feature_library_impl_my_daily_vip),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.feature_library_impl_my_daily_vip_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Chevron(Modifier.offset(x = 4.dp).testTag("my-daily-vip-trailing"))
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    ResonoteIconButton(
        label = stringResource(R.string.feature_library_impl_settings),
        onClick = onClick,
        modifier = Modifier.offset(x = 12.dp).testTag("my-settings"),
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
private fun Avatar(profile: UserProfile) {
    Surface(
        modifier = Modifier.size(64.dp).testTag("my-avatar"),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Box(Modifier.clip(CircleShape), contentAlignment = Alignment.Center) {
            Text(
                profile.nickname.take(1).ifBlank { "·" },
                style = MaterialTheme.typography.titleLarge,
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
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(Modifier.width(88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
internal fun Chevron(modifier: Modifier = Modifier) {
    Icon(
        Icons.Rounded.ChevronRight,
        null,
        modifier,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
