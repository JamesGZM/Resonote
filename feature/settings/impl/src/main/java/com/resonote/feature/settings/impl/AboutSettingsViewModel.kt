package com.resonote.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.AppUpdateRepository
import com.resonote.core.model.AppRelease
import com.resonote.core.model.CollectionLoadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AboutUpdateState {
    data object Checking : AboutUpdateState
    data class Latest(val version: String) : AboutUpdateState
    data class Available(val release: AppRelease) : AboutUpdateState
    data object Failed : AboutUpdateState
}

@HiltViewModel
class AboutSettingsViewModel @Inject constructor(private val appUpdateRepository: AppUpdateRepository) : ViewModel() {
    private val mutableUpdateState = MutableStateFlow<AboutUpdateState>(AboutUpdateState.Checking)
    val updateState: StateFlow<AboutUpdateState> = mutableUpdateState.asStateFlow()
    private var checkedVersion: String? = null

    fun checkForUpdates(currentVersion: String, force: Boolean = false) {
        if (!force && checkedVersion == currentVersion) return
        if (currentVersion.isBlank()) {
            mutableUpdateState.value = AboutUpdateState.Failed
            return
        }
        checkedVersion = currentVersion
        mutableUpdateState.value = AboutUpdateState.Checking
        viewModelScope.launch {
            try {
                mutableUpdateState.value = when (val result = appUpdateRepository.latestRelease()) {
                    is CollectionLoadResult.Available -> {
                        if (AppVersionComparator.isNewer(result.value.version, currentVersion)) {
                            AboutUpdateState.Available(result.value)
                        } else {
                            AboutUpdateState.Latest(result.value.version)
                        }
                    }
                    is CollectionLoadResult.Failed -> AboutUpdateState.Failed
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUpdateState.value = AboutUpdateState.Failed
            }
        }
    }
}

internal object AppVersionComparator {
    fun isNewer(latest: String, current: String): Boolean {
        val latestVersion = ParsedVersion.parse(latest)
        val currentVersion = ParsedVersion.parse(current)
        return if (latestVersion != null && currentVersion != null) {
            latestVersion > currentVersion
        } else {
            normalize(latest) != normalize(current)
        }
    }

    private fun normalize(value: String): String = value.trim().removePrefix("v").removePrefix("V")

    private data class ParsedVersion(val numbers: List<Int>, val prerelease: List<String>?) :
        Comparable<ParsedVersion> {
        override fun compareTo(other: ParsedVersion): Int {
            repeat(maxOf(numbers.size, other.numbers.size)) { index ->
                val comparison = (numbers.getOrNull(index) ?: 0).compareTo(other.numbers.getOrNull(index) ?: 0)
                if (comparison != 0) return comparison
            }
            if (prerelease == null && other.prerelease != null) return 1
            if (prerelease != null && other.prerelease == null) return -1
            val left = prerelease ?: return 0
            val right = other.prerelease ?: return 0
            repeat(maxOf(left.size, right.size)) { index ->
                val leftPart = left.getOrNull(index) ?: return -1
                val rightPart = right.getOrNull(index) ?: return 1
                val comparison = comparePrereleasePart(leftPart, rightPart)
                if (comparison != 0) return comparison
            }
            return 0
        }

        companion object {
            private val pattern = Regex(
                "^[vV]?(\\d+(?:\\.\\d+)*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$",
            )

            fun parse(value: String): ParsedVersion? {
                val match = pattern.matchEntire(value.trim()) ?: return null
                val numbers = match.groupValues[1].split('.').map { it.toIntOrNull() ?: return null }
                val prerelease = match.groupValues[2].takeIf(String::isNotEmpty)?.split('.')
                return ParsedVersion(numbers, prerelease)
            }

            private fun comparePrereleasePart(left: String, right: String): Int {
                val leftNumber = left.toIntOrNull()
                val rightNumber = right.toIntOrNull()
                return when {
                    leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                    leftNumber != null -> -1
                    rightNumber != null -> 1
                    else -> left.compareTo(right)
                }
            }
        }
    }
}
